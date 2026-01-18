"use client"

import Link from "next/link";
import { useRouter } from "next/navigation";

export default function PublicHeader() {
    const router = useRouter();

    async function handleAccessAccount() {
        try {
            const res = await fetch("/api/me", { credentials: "include" });
            if (res.ok) {
                router.push("/dashboard");
            } else {
                router.push("/login");
            }
        } catch {
            router.push("/login");
        }
    }

    return (
        <header className="px-5 lg:px-10 py-6 flex justify-between items-center gap-4 lg:gap-0">
            <Link href="/" className="w-auto lg:w-[22%]">
                UrbanPay
            </Link>
            <div className="w-[54%] hidden items-center justify-center lg:flex">
                <nav className="flex flex-row items-center justify-center gap-6 2xl:gap-8 whitespace-nowrap">
                    <a href="/products/account" className="hidden xl:flex">Conta</a>
                    <a href="/products/cards">Cartão de Crédito</a>
                    <a href="/products/piggys">Porquinho</a>
                    <a href="/products/loans">Empréstimos e Financiamentos</a>
                    <a href="/products/investments">Investimentos</a>
                    <a href="/products/machine">Maquininha</a>
                </nav>
            </div>
            <div className="w-auto lg:w-[22%] flex flex-row items-center gap-4 justify-end">
                <Link href="/register" className="cursor-pointer">
                    <button className="cursor-pointer text-white bg-blue-600 border-2 border-blue-600 px-4 py-2 rounded-lg hover:bg-blue-500 transition font-semibold">
                        Abra sua conta
                    </button>
                </Link>
                <button onClick={handleAccessAccount} className="cursor-pointer border-2 border-blue-600 text-blue-600 px-4 py-2 rounded-lg hover:bg-blue-100 transition font-semibold">
                    Acesse sua conta
                </button>
                <Link href="/help/contactus" className="cursor-pointer size-10 aspect-square hidden 2xl:flex">
                    <img src="/help-icon.png" alt="Ícone de Ajuda" className="size-10 aspect-square object-contain" draggable="false"></img>
                </Link>
            </div>
        </header>
    )
}