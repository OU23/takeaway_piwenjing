import http from './http'

/* ------------------------------ 骑手端 ------------------------------ */
export const loginRider = (data) => http.post('/rider/login', data, { silent: true })
export const getRiderProfile = () => http.get('/rider/profile')
export const setRiderOnline = (online) =>
  http.post('/rider/profile/online', null, { params: { online } })

export const getRiderHall = () => http.get('/rider/hall')
export const getRiderTasks = (params) => http.get('/rider/tasks', { params })
export const getRiderTask = (orderNo) => http.get(`/rider/tasks/${orderNo}`)
export const grabOrder = (orderNo) => http.post(`/rider/tasks/${orderNo}/grab`)
export const pickupOrder = (orderNo) => http.post(`/rider/tasks/${orderNo}/pickup`)
export const deliverOrder = (orderNo) => http.post(`/rider/tasks/${orderNo}/deliver`)

export const getRiderIncome = () => http.get('/rider/income')
export const getRiderMessages = () => http.get('/rider/messages')
export const readRiderMessage = (id) => http.post(`/rider/messages/${id}/read`)
