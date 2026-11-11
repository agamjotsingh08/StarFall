// Original deterministic synthesized sound effects. No external sample licenses.
import {mkdirSync,writeFileSync} from 'node:fs';
const dir=new URL('../app/src/main/res/raw/',import.meta.url); mkdirSync(dir,{recursive:true});
let seed=1234;
function noise(){ seed=(Math.imul(seed,1664525)+1013904223)|0; return (seed>>>0)/2147483648-1; }
function wav(name,duration,fn){
  const rate=22050,n=Math.ceil(duration*rate),b=Buffer.alloc(44+n*2);
  b.write('RIFF');b.writeUInt32LE(36+n*2,4);b.write('WAVEfmt ',8);b.writeUInt32LE(16,16);
  b.writeUInt16LE(1,20);b.writeUInt16LE(1,22);b.writeUInt32LE(rate,24);b.writeUInt32LE(rate*2,28);
  b.writeUInt16LE(2,32);b.writeUInt16LE(16,34);b.write('data',36);b.writeUInt32LE(n*2,40);
  for(let i=0;i<n;i++){const t=i/rate,u=t/duration,v=fn(t,u)*Math.min(1,t*300)*(1-u);b.writeInt16LE(Math.round(Math.max(-1,Math.min(1,v))*.8*32767),44+i*2);}
  writeFileSync(new URL(`${name}.wav`,dir),b);
}
const tau=Math.PI*2;
wav('fire',.1,(t,u)=>Math.sin(tau*(1300*t-4200*t*t))*.4*Math.exp(-u*4));
wav('hit',.06,(t,u)=>(noise()*.3+Math.sin(tau*180*t)*.3)*Math.exp(-u*6));
wav('explosion',.52,(t,u)=>(noise()*.6+Math.sin(tau*(90*t-50*t*t))*.4)*Math.exp(-u*5));
wav('pickup',.42,(t,u)=>Math.sin(tau*[523.25,659.25,783.99,1046.5][Math.min(3,Math.floor(u*4))]*t)*.5);
wav('hurt',.3,(t,u)=>(Math.sin(tau*110*t)*.35+noise()*.35)*Math.exp(-u*3));
wav('nova',.85,(t,u)=>(Math.sin(tau*(150*t-65*t*t))*.5+noise()*.35)*Math.exp(-u*3));
console.log('Created six original PCM effects.');
