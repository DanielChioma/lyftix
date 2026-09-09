import { API_BASE_URL } from './config';import{codingHistoryQueryKey,getCodingHistory}from'./coding'
const p={startDate:'2026-09-01',endDate:'2026-09-30',page:2,size:20,sortBy:'language' as const};afterEach(()=>vi.unstubAllGlobals())
it('sends exact half-open history parameters',async()=>{vi.stubGlobal('fetch',vi.fn().mockResolvedValue(new Response('{}',{status:200})));await getCodingHistory(p);expect(fetch).toHaveBeenCalledWith(`${API_BASE_URL}/api/coding-sessions/filter?start=2026-09-01T00%3A00%3A00.000Z&end=2026-10-01T00%3A00%3A00.000Z&page=2&size=20&sortBy=language`,expect.any(Object))})
it('keys history by all server state',()=>expect(codingHistoryQueryKey(p)).toEqual(['coding','history','2026-09-01','2026-09-30',2,20,'language']))
