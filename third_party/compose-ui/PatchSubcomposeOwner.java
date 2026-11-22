// SPDX-License-Identifier: Apache-2.0
import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PatchSubcomposeOwner implements Opcodes {
    private static final String STATE = "androidx/compose/ui/layout/LayoutNodeSubcompositionsState";
    private static final String NODE = "androidx/compose/ui/node/LayoutNode";
    private static final String OWNER = "androidx/compose/ui/node/Owner";
    private static final String EXECUTOR = "Landroidx/compose/ui/node/OutOfFrameExecutor;";

    public static void main(String[] args) throws Exception {
        ClassReader reader = new ClassReader(Files.readAllBytes(Path.of(args[0])));
        if (!reader.getClassName().equals(STATE)) throw new IllegalStateException("Unexpected class");
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] patched = {0};
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor destination = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals("getOutOfFrameExecutor") || !descriptor.equals("()" + EXECUTOR)) return destination;
                return new MethodNode(ASM9, access, name, descriptor, signature, exceptions) {
                    @Override
                    public void visitEnd() {
                        int ownerCalls = 0;
                        for (AbstractInsnNode instruction : instructions) {
                            if (instruction instanceof MethodInsnNode method && method.getOpcode() == INVOKESTATIC &&
                                method.owner.equals("androidx/compose/ui/node/LayoutNodeKt") && method.name.equals("requireOwner")) {
                                ownerCalls++;
                            }
                        }
                        if (ownerCalls != 1) throw new IllegalStateException("Unexpected original getter");
                        instructions.clear();
                        tryCatchBlocks.clear();
                        if (localVariables != null) localVariables.clear();
                        visibleLocalVariableAnnotations = null;
                        invisibleLocalVariableAnnotations = null;
                        LabelNode start = new LabelNode();
                        LabelNode attached = new LabelNode();
                        instructions.add(start);
                        instructions.add(new LineNumberNode(664, start));
                        instructions.add(new VarInsnNode(ALOAD, 0));
                        instructions.add(new FieldInsnNode(GETFIELD, STATE, "root", "L" + NODE + ";"));
                        instructions.add(new MethodInsnNode(INVOKEVIRTUAL, NODE, "getOwner$androidx_compose_ui_ui", "()L" + OWNER + ";", false));
                        instructions.add(new InsnNode(DUP));
                        instructions.add(new JumpInsnNode(IFNONNULL, attached));
                        instructions.add(new InsnNode(POP));
                        instructions.add(new InsnNode(ACONST_NULL));
                        instructions.add(new InsnNode(ARETURN));
                        instructions.add(attached);
                        instructions.add(new FrameNode(F_SAME1, 0, null, 1, new Object[] {OWNER}));
                        instructions.add(new MethodInsnNode(INVOKEINTERFACE, OWNER, "getOutOfFrameExecutor", "()" + EXECUTOR, true));
                        instructions.add(new InsnNode(ARETURN));
                        maxStack = 2;
                        maxLocals = 1;
                        accept(destination);
                        patched[0]++;
                    }
                };
            }
        }, 0);
        if (patched[0] != 1) throw new IllegalStateException("Getter was not patched exactly once");
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
