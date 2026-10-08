import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const text = readFileSync(new URL('../src/utils/consultation-view.js', import.meta.url), 'utf8');
const {normalizeWords, reportText} = await import('data:text/javascript;base64,' + Buffer.from(text).toString('base64'));
test('backend word cloud names and counts render with normalized sizing', () => {
 const result=normalizeWords([{name:'数据共享',value:12},{name:'个人信息',value:3},{name:'',value:8}]);
 assert.deepEqual(result,[{word:'数据共享',count:12,weight:1},{word:'个人信息',count:3,weight:.25}]);
 assert.deepEqual(normalizeWords([]),[]);
});
test('report uses complete server statistics rather than loaded opinion count', () => {
 const result=reportText({consultation:{title:'公开征集',startDate:'2026-10-01',endDate:'2026-10-31'},statistics:{totalOpinions:42,byCategory:{数据:{total:30,support:20,oppose:5,neutral:5}}}});
 assert.match(result,/意见总数：42/); assert.match(result,/数据：30 条/); assert.equal(reportText({content:'正文'}),'正文');
});
