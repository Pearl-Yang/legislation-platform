/**
 * 公共 request 封装 - 统一调用后端 API
 *
 * 后端基础地址：
 *   - 真机调试时改为局域网 IP（如 http://192.168.1.10:8083/api）
 *   - 微信开发者工具勾选"不校验合法域名"即可使用 localhost
 */
const BASE_URL = 'http://localhost:8083/api'

const request = (options) => {
  const { url, method = 'GET', data, header = {}, hideError = false } = options
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      timeout: 15000,
      header: {
        'X-User-Id': uni.getStorageSync('userId') || 1,
        'X-Role':    uni.getStorageSync('userRole') || '系统管理员',
        ...header
      },
      success: (res) => {
        const body = res.data || {}
        if (res.statusCode !== 200) {
          !hideError && uni.showToast({ title: '网络异常(' + res.statusCode + ')', icon: 'none' })
          reject(new Error('HTTP ' + res.statusCode))
          return
        }
        if (body.code === 200) {
          resolve(body.data)
        } else {
          !hideError && uni.showToast({ title: body.message || '操作失败', icon: 'none' })
          reject(new Error(body.message || '操作失败'))
        }
      },
      fail: (err) => {
        !hideError && uni.showToast({ title: '请求未发送成功', icon: 'none' })
        reject(err)
      }
    })
  })
}

export { BASE_URL, request }
export default request