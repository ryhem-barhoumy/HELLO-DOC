import {build} from 'esbuild';
import {mkdir} from 'node:fs/promises';
import {spawnSync} from 'node:child_process';
await mkdir('.angular/agenda-account-tests',{recursive:true});
await build({entryPoints:['src/app/features/appointments/agenda-account.test.ts'],outfile:'.angular/agenda-account-tests/test.mjs',bundle:true,platform:'node',format:'esm',packages:'external',tsconfig:'tsconfig.json'});
process.exit(spawnSync(process.execPath,['--import','@angular/compiler','--test','.angular/agenda-account-tests/test.mjs'],{stdio:'inherit'}).status??1);
