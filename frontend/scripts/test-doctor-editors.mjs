import {build} from 'esbuild';
import {mkdir} from 'node:fs/promises';
import {spawnSync} from 'node:child_process';
await mkdir('.angular/doctor-editor-tests',{recursive:true});
await build({entryPoints:['src/app/features/doctor/doctor-editors.test.ts'],outfile:'.angular/doctor-editor-tests/test.mjs',bundle:true,platform:'node',format:'esm',packages:'external',tsconfig:'tsconfig.json'});
process.exit(spawnSync(process.execPath,['--import','@angular/compiler','--test','.angular/doctor-editor-tests/test.mjs'],{stdio:'inherit'}).status??1);
