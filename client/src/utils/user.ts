import { GLOBAL_CONFIG } from '../config/global'
import { IMinimalUser } from '../requests/responses/user'

export function getAvatar(user: IMinimalUser) {
  return user.avatar
    ? `${GLOBAL_CONFIG.server_host}/api/v2/avatars/${user.userId}?filename=${user.avatar}`
    : undefined
}

export function getAvatarPath(user: IMinimalUser) {
  return user.avatar ? `/v2/avatars/${user.userId}?filename=${user.avatar}` : undefined
}
