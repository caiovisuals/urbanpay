export type User = {
    id: number;
    fullName: string;
    avatarUrl?: string;
    roles?: UserRole[];
    email: string;
    phone: string;
    password: string;
    cpf: string;
    rg?: string;
    birthDate: Date;
    pin: string;
    balance: number;
    transactions?: Transaction[];
    piggys?: Piggy[];
    cards?: Card[];
    loans?: Loan[];
    investments?: Investment[];
    emailVerified?: boolean;
    phoneVerified?: boolean;
    lastLogin?: Date;
    token?: string;
    createdAt: Date;
};

export type Notification = {
    id: number;
    type: "transaction" | "security" | "system";
    message: string;
    read: boolean;
    createdAt: Date;
};

export type TransactionStatus = "pending" | "completed" | "failed" | "cancelled";

export type Transaction = {
    id: number;
    status: TransactionStatus;
    method?: "pix" | "ted" | "doc" | "boleto" | "card" | "internal";
    value: number;
    date: Date;
    description?: string;
    fromUserId?: number;
    toUserId?: number;
};

export type Piggy = {
    id: number;
    name: string;
    value: number;
    goal?: number;
    createdAt: Date;
};

export type CardStatus = "active" | "blocked" | "expired" | "cancelled";

export type Card = {
    id: number;
    type: "platinum" | "business" | "black";
    status: CardStatus;
    number: string;
    name: string;
    expiry: string;
    cvv: string;
    limit: number;
    balance: number;
    createdAt: Date;
};

export type LoanStatus = "requested" | "approved" | "rejected" | "active" | "paid" | "default";

export type Loan = {
    id: number;
    status: LoanStatus;
    amount: number;
    interestRate: number;
    installments: number;
    remainingDebt?: number;
    requestedAt: Date;
    approvedAt?: Date;
    paidAt?: Date;
};

export type InvestmentStatus = "active" | "closed" | "pending";

export type Investment = {
    id: number;
    status: InvestmentStatus;
    amount: number;
    currentValue: number;
    profitLoss?: number;
    startDate: Date;
    endDate?: Date;
    description?: string;
};

export enum UserRole {
    USER = "user",
    ADMIN = "admin",
    SUPPORT = "support",
};

export const users: User[] = [];