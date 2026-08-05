// Gera o som de alerta de ponto: um padrão de sirene de dois tons, alto e
// característico (diferente do som padrão do aparelho). Saída: WAV PCM 16-bit
// mono 44.1kHz, em android/app/src/main/res/raw/ e ios/Runner/.
//   uso:  node tool/gen_alerta_sound.js   (a partir da pasta mobile/)
const fs = require('fs');
const path = require('path');

const SR = 44100;
const samples = [];

function tom(freq, durSec, amp) {
  const n = Math.floor(SR * durSec);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    // envelope curto de ataque/decaimento (5 ms) para não estalar
    const env = Math.min(1, i / (0.005 * SR), (n - i) / (0.005 * SR));
    let s = amp * env * Math.sin(2 * Math.PI * freq * t);
    samples.push(Math.max(-1, Math.min(1, s)));
  }
}

const A = 0.92; // alto
for (let r = 0; r < 3; r++) {
  tom(880, 0.18, A);   // lá
  tom(1320, 0.18, A);  // mi agudo
}
tom(1320, 0.55, A);    // toque final sustentado

const buf = Buffer.alloc(44 + samples.length * 2);
buf.write('RIFF', 0);
buf.writeUInt32LE(36 + samples.length * 2, 4);
buf.write('WAVE', 8);
buf.write('fmt ', 12);
buf.writeUInt32LE(16, 16);
buf.writeUInt16LE(1, 20);   // PCM
buf.writeUInt16LE(1, 22);   // mono
buf.writeUInt32LE(SR, 24);
buf.writeUInt32LE(SR * 2, 28);
buf.writeUInt16LE(2, 32);
buf.writeUInt16LE(16, 34);
buf.write('data', 36);
buf.writeUInt32LE(samples.length * 2, 40);
for (let i = 0; i < samples.length; i++) {
  buf.writeInt16LE(Math.round(samples[i] * 32767), 44 + i * 2);
}

const alvos = [
  'android/app/src/main/res/raw/alerta_ponto.wav',
  'ios/Runner/alerta_ponto.wav',
];
for (const rel of alvos) {
  const p = path.join(process.cwd(), rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, buf);
  console.log('gravado', rel, buf.length, 'bytes,', (samples.length / SR).toFixed(2), 's');
}
