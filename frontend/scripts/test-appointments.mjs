import { build } from 'esbuild';
import { mkdir } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
await mkdir('.angular/appointment-tests',{recursive:true});
await build({entryPoints:['src/app/features/appointments/appointment-actions.test.ts'],outfile:'.angular/appointment-tests/test.mjs',bundle:true,platform:'node',format:'esm',packages:'external',tsconfig:'tsconfig.json'});
const result=spawnSync(process.execPath,['--test','.angular/appointment-tests/test.mjs'],{stdio:'inherit'});
process.exit(result.status??1);
