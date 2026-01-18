import Link from "next/link"

export default function HomeFooter() {
    return (
        <footer className="px-5 md:px-20 lg:px-30 xl:px-50 py-24 flex flex-col gap-8">
                <div className="flex flex-row gap-15 xl:gap-25">
                    <div className="flex flex-col gap-2">
                        <span className="font-semibold whitespace-nowrap">LINKS ÚTEIS</span>
                        <nav className="flex flex-col">
                            <a href="/products/account">Conta</a>
                            <a href="/products/piggys">Porquinho</a>
                            <a href="/products/cards">Cartão de Crédito</a>
                            <a href="/products/loans">Empréstimos e Financiamentos</a>
                            <a href="/products/investments">Investimentos</a>
                            <a href="/products/machine">Maquininha</a>
                        </nav>
                    </div>
                    <div className="flex flex-col gap-2">
                        <span className="font-semibold whitespace-nowrap">DÚVIDAS</span>
                        <nav className="flex flex-col">
                            <a href="/help/contactus">Fale conosco</a>
                            <a href="/help/negotiateyourdebt">Negocie sua dívida</a>
                        </nav>
                    </div>
                    <div className="flex flex-col gap-2">
                        <span className="font-semibold whitespace-nowrap">SOBRE NÓS</span>
                        <nav className="flex flex-col">
                            <a href="/about/whoweare">Quem Somos</a>
                            <a href="/about/workwithus">Trabalhe Conosco</a>
                            <a href="/about/devsranch">Rancho dos Devs</a>
                        </nav>
                    </div>
                    <div className="flex flex-col gap-2">
                        <span className="font-semibold whitespace-nowrap">REDES SOCIAIS</span>
                        <nav className="flex flex-row gap-2">
                            <a href="https://instagram.com">
                                <img src="social-media-icons/instagram.png" alt="Instagram" className="size-7 object-contain aspect-square" draggable="false"/>
                            </a>
                            <a href="https://tiktok.com">
                                <img src="social-media-icons/tiktok.png" alt="TikTok" className="size-7 object-contain aspect-square" draggable="false"/>
                            </a>
                            <a href="https://youtube.com">
                                <img src="social-media-icons/youtube.png" alt="Youtube" className="size-7 object-contain aspect-square" draggable="false"/>
                            </a>
                        </nav>
                    </div>
                </div>
                <div className="flex flex-row justify-between items-end">
                    <div className="flex flex-col gap-5">
                        <h1>Logo</h1>
                        <p className="whitespace-nowrap">© {new Date().getFullYear()} Urbanpay</p>
                        <div className="whitespace-nowrap">
                            <p><span className="font-semibold">CNPJ: </span>00.000.000/0000-00</p>
                            <p>Avenida Oceanica, Salvador, Bahia - 00000-000</p>
                        </div>
                    </div>
                    <div className="flex flex-row gap-8">
                        <Link href="/rates">Tarifas</Link>
                        <Link href="/terms">Termos de Uso</Link>
                        <Link href="/security">Segurança</Link>
                    </div>
                </div>
        </footer>
    )
}