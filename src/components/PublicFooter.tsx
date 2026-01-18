import Link from "next/link";

export default function PublicFooter() {
    return (
        <footer className="px-85 py-24 flex flex-row justify-between items-end">
            <div className="flex flex-col gap-5">
                <h1>Logo</h1>
                <p>© {new Date().getFullYear()} UrbanPay</p>
                <div>
                    <p><span className="font-semibold">CNPJ: </span>00.000.000/0000-00</p>
                    <p>Avenida Oceanica, Salvador, Bahia - 00000-000</p>
                </div>
            </div>
            <div className="flex flex-row gap-8">
                <Link href="/rates">Tarifas</Link>
                <Link href="/terms">Termos de Uso</Link>
                <Link href="/security">Segurança</Link>
            </div>
        </footer>
    )
}