import { callCoreAsUser } from "@/_lib/core/bff"

type Account = { id: string; currency: string; balance: string }

/**
 * Somente leitura. A versão antiga aceitava POST { value, type: "inflow" } e qualquer
 * usuário podia criar dinheiro. Agora saldo só muda por lançamentos no ledger do core.
 */
export async function GET() {
    return callCoreAsUser<Account>("/api/v1/accounts/me", {}, (a) => ({
        accountId: a.id,
        currency: a.currency,
        balance: a.balance,
    }))
}
