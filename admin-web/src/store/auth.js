import { reactive, computed } from 'vue'

const STORAGE_KEY = 'xg_admin_auth'

// S6 修复：演示账号清单与「直接改 token 切换角色」已移除；
// 角色只能由登录接口的返回值决定（token 为后端签发的签名令牌）。

const state = reactive({
  token: '',
  name: '',
  role: '',
  roleLabel: ''
})

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    token: state.token, name: state.name, role: state.role, roleLabel: state.roleLabel
  }))
}

export function restoreAuth() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      Object.assign(state, JSON.parse(raw))
    } catch (e) {
      /* ignore */
    }
  }
}

export const auth = state

export const isLoggedIn = computed(() => !!state.token)

// 是否具备管理（写）权限：超级管理员 / 运营 可操作，客服仅只读
export const canManage = computed(() => state.role === 'SUPER_ADMIN' || state.role === 'OPERATOR')

export function setAuth({ token, name, role, roleLabel }) {
  state.token = token
  state.name = name
  state.role = role
  state.roleLabel = roleLabel
  persist()
}

export function logout() {
  state.token = ''
  state.name = ''
  state.role = ''
  state.roleLabel = ''
  localStorage.removeItem(STORAGE_KEY)
}
