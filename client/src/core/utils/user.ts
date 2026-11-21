import { GLOBAL_CONFIG } from '@/core/config/global'
import type { IMinimalUser } from '@/core/user/requests/responses/user'

export function getAvatar(user: IMinimalUser) {
  return user.avatar
    ? `${GLOBAL_CONFIG.server_host}/api/v2/avatars/${user.userId}?filename=${user.avatar}`
    : undefined
}

export function getAvatarPath(user: IMinimalUser) {
  return user.avatar ? `/v2/avatars/${user.userId}?filename=${user.avatar}` : undefined
}
