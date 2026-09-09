import { apiRequest } from './client'
import type { CodingHistoryParameters, CodingPage } from './coding.types'
import { startOfFollowingUtcDay, startOfUtcDay } from '../utils/dateRange'
export async function getCodingHistory(p:CodingHistoryParameters){const q=new URLSearchParams({start:startOfUtcDay(p.startDate),end:startOfFollowingUtcDay(p.endDate),page:String(p.page),size:String(p.size),sortBy:p.sortBy});return(await apiRequest<CodingPage>(`/api/coding-sessions/filter?${q}`)).data}
export const codingHistoryQueryKey=(p:CodingHistoryParameters)=>['coding','history',p.startDate,p.endDate,p.page,p.size,p.sortBy] as const
