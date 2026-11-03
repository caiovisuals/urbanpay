import { callCoreAsUser } from "@/_lib/core/bff"
import type { User } from "@/_lib/types"

export async function GET() {
    return callCoreAsUser<User>("/api/v1/me", {}, (user) => ({ user }))
}