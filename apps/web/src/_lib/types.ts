/**
 * Tipos de domínio usados pelas telas.
 * A fonte da verdade do saldo e do extrato agora é o serviço Java (services/core);
 * os tipos abaixo de produtos ainda não migrados (cartões, porquinhos etc.) seguem aqui.
 */

export type User = {
    id: string
    fullName: string
    avatarUrl?: string
    email: string
    phone: string
    cpfMasked?: string
    createdAt: string
}

/** Valores monetários chegam como string decimal ("10.50") para não perder precisão. */
export type StatementItem = {
    id: number
    entryId: string
    type: "DEPOSIT" | "TRANSFER"
    description: string | null
    amount: string
    balanceAfter: string
    createdAt: string
}

export type Transaction = StatementItem

export type Piggy = {
    id: number
    name: string
    value: number
    goal?: number
    createdAt: Date
}
