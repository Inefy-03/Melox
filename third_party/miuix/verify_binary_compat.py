import struct,zipfile,io
from pathlib import Path

def parse(data):
 p=8
 def u1():
  nonlocal p
  v=data[p];p+=1;return v
 def u2():
  nonlocal p
  v=struct.unpack_from('>H',data,p)[0];p+=2;return v
 def u4():
  nonlocal p
  v=struct.unpack_from('>I',data,p)[0];p+=4;return v
 cp=[None]*u2();i=1
 while i<len(cp):
  t=u1()
  if t==1:n=u2();cp[i]=(t,data[p:p+n].decode('utf-8','replace'));p+=n
  elif t in [3,4]:p+=4
  elif t in [5,6]:p+=8;i+=1
  elif t in [7,8,16,19,20]:cp[i]=(t,u2())
  elif t in [9,10,11,12,17,18]:cp[i]=(t,u2(),u2())
  elif t==15:p+=3
  else:raise Exception(t)
  i+=1
 def string(index):return cp[index][1]
 u2();owner=string(cp[u2()][1]);u2()
 for _ in range(u2()):u2()
 def member():
  acc=u2();name=string(u2());desc=string(u2())
  for _ in range(u2()):u2();n=u4();advance(n)
  return name,desc,acc
 def advance(n):
  nonlocal p
  p+=n
 for _ in range(u2()):member()
 methods={ (n,d) for n,d,flags in (member() for _ in range(u2())) }
 refs=[]
 for x in cp:
  if x and x[0] in [10,11]:
   cls=string(cp[x[1]][1]);nt=cp[x[2]];refs.append((cls,string(nt[1]),string(nt[2])))
 return owner,methods,refs

def load(path):
 with zipfile.ZipFile(path) as z:
  if 'classes.jar' in z.namelist():return fromzip(zipfile.ZipFile(io.BytesIO(z.read('classes.jar'))))
  return fromzip(z)
def fromzip(z):return {o:(m,r) for o,m,r in (parse(z.read(n)) for n in z.namelist() if n.endswith('.class'))}
if __name__ == "__main__":
 import argparse
 parser=argparse.ArgumentParser(description="Check existing JVM method references against a replacement Miuix UI AAR.")
 parser.add_argument("baseline",type=Path)
 parser.add_argument("candidate",type=Path)
 parser.add_argument("dependencies",nargs="+",type=Path)
 args=parser.parse_args()
 old=load(args.baseline)
 ui=load(args.candidate)
 missing=set()
 for path in args.dependencies:
  for owner,(methods,refs) in load(path).items():
   for cls,n,d in refs:
    if cls in old and (n,d) in old[cls][0] and (cls not in ui or (n,d) not in ui[cls][0]):
     missing.add((path.name,owner,cls,n,d))
 for entry in sorted(missing):print(*entry,sep="\n  ")
 print("Missing existing UI method references:",len(missing))
 raise SystemExit(bool(missing))
