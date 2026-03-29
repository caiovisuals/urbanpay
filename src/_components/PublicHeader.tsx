"use client"

import Link from "next/link"
import Image from "next/image"
import { useRouter } from "next/navigation"

export default function PublicHeader() {
    const router = useRouter()

    async function handleAccessAccount() {
        try {
            const res = await fetch("/api/me", { credentials: "include" })
            if (res.ok) {
                router.push("/dashboard")
            } else {
                router.push("/login")
            }
        } catch {
            router.push("/login")
        }
    }

    return (
        <header className="flex justify-between items-center bg-[var(--black-background)] text-[var(--white-text)] gap-4 px-5 lg:px-10 py-6">
            <div className="w-[20%]">
                <Link href="/" className="inline-block w-fit">
                    <div className="w-32 aspect-[2838/760] relative">
                        <Image src="/assets/white-logo.png" alt="Urb Logo" className="select-none object-contain" draggable="false" fill />
                    </div>
                </Link>
            </div>
            <div className="w-[50%] hidden items-center justify-end lg:flex">
                <nav className="flex flex-row items-center justify-center gap-6 2xl:gap-8 whitespace-nowrap">
                    <Link href="/products/account">Conta</Link>
                    <Link href="/products/cards" className="hidden sm:block">Cartão de Crédito</Link>
                    <Link href="/products/piggys" className="hidden md:block">Porquinho</Link>
                    <Link href="/products/loans" className="hidden lg:block">Empréstimos e Financiamentos</Link>
                    <Link href="/products/investments" className="hidden xl:block">Investimentos</Link>
                    <Link href="/products/machine" className="hidden 2xl:block">Maquininha</Link>
                </nav>
            </div>
            <div className="w-auto lg:w-[28%] flex flex-row items-center gap-4 justify-end">
                <Link href="/register" className="bg-blue-600 border-2 border-blue-600 px-4 py-2 rounded-lg text-[var(--white-text)] hover:bg-blue-500 transition-normal font-semibold whitespace-nowrap">
                    Abra sua conta
                </Link>
                <button onClick={handleAccessAccount} className="border-2 border-blue-600 px-4 py-2 rounded-lg text-[var(--white-text)] hover:bg-blue-100 transition-normal font-semibold whitespace-nowrap">
                    Acesse sua conta
                </button>
                <Link href="/help/contactus" className="size-10 aspect-square hidden 2xl:flex">
                    <svg viewBox="0 0 24 24" fill="none" stroke="var(--white-text)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
                    </svg>
                </Link>
            </div>
        </header>
    )
}