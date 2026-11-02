"use client"

import Link from "next/link"
import { useState } from "react"
import PublicHeader from "@/_components/PublicHeader"
import PublicFooter from "@/_components/PublicFooter"

export default function LandingPage() {
    const [activeTab, setActiveTab] = useState<"personal" | "business">("personal")

    return (
        <div className="flex flex-col">
            <PublicHeader />
            <main className="size-full flex flex-col bg-gradient-to-br from-blue-50 to-white text-gray-900">
                <section className="flex flex-col items-center justify-center text-center px-5 md:px-20 lg:px-40 xl:px-70 2xl:px-100 h-[calc(100vh-92px)]">
                    <h1 className="text-5xl md:text-6xl font-extrabold leading-none mb-6">
                        Seu banco digital, simples, transparente e baiano.
                    </h1>
                    <p className="text-lg md:text-xl text-gray-600 max-w-2xl mb-8">
                        Controle suas finanças, crie cofrinhos, faça transferências instantâneas e acompanhe tudo em tempo real — sem taxas escondidas.
                    </p>
                    <div className="flex gap-4">
                        <Link href="/register" className="cursor-pointer">
                            <button className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-2xl shadow-lg transition cursor-pointer">
                                Abrir conta gratuita
                            </button>
                        </Link>
                        <button className="px-6 py-3 bg-gray-100 hover:bg-gray-200 text-gray-900 font-medium rounded-2xl shadow-lg transition cursor-pointer">
                            Saiba mais
                        </button>
                    </div>
                </section>
                <section className="px-5 py-20 bg-blue-600 text-white flex flex-col items-center gap-5">
                    <h2 className="text-3xl md:text-4xl font-bold mb-6">Tudo que você precisa de um banco. Em um só banco.</h2>
                    <div className="max-w-6xl mx-auto grid md:grid-cols-3 gap-12">
                        <div className="p-6 bg-blue-700 rounded-2xl shadow">
                            <h3 className="text-xl font-semibold mb-2">Transações instantâneas</h3>
                            <p>Faça PIX, TED e DOC em segundos, com segurança e sem complicação.</p>
                        </div>
                        <div className="p-6 bg-blue-700 rounded-2xl shadow">
                            <h3 className="text-xl font-semibold mb-2">Cofrinhos inteligentes</h3>
                            <p>Crie objetivos financeiros, acompanhe metas e organize suas economias.</p>
                        </div>
                        <div className="p-6 bg-blue-700 rounded-2xl shadow">
                            <h3 className="text-xl font-semibold mb-2">Cartões virtuais e físicos</h3>
                            <p>Use cartões de débito e crédito com limite personalizado e segurança.</p>
                        </div>
                    </div>
                </section>
                <section className="px-5 py-24 flex flex-col items-center justify-center gap-5">
                    <h2 className="text-3xl md:text-4xl font-bold mb-6">Escolha o seu cartão</h2>
                    <div className="flex flex-col md:flex-row gap-10 justify-center">
                        <div className="bg-amber-200 p-8 rounded-2xl">
                            <img src="" alt="" draggable="false"></img>
                            <h2>Platinum</h2>
                            <p>Crédito e debito, pontos e alto limte.</p>
                            <button className="mt-6">Quero ter esse</button>
                        </div>
                        <div className="bg-amber-200 p-8 rounded-2xl">
                            <img src="" alt="" draggable="false"></img>
                            <h2>Business</h2>
                            <p>Crédito e debito, pontos e alto limte.</p>
                            <button className="mt-6">Quero ter esse</button>
                        </div>
                        <div className="bg-amber-200 p-8 rounded-2xl">
                            <img src="" alt="" draggable="false"></img>
                            <h2>Black</h2>
                            <p>Crédito e debito, pontos e alto limte.</p>
                            <button className="mt-6">Quero ter esse</button>
                        </div>
                    </div>
                </section>
                <section className="px-5 py-24 flex flex-col items-center justify-center gap-5 bg-blue-300">
                    <div className="mb-6">
                        <h2 className="text-3xl md:text-4xl font-bold">Mais de 1.000 investimentos ao seu alcance!</h2>
                        <h3 className="text-2xl md:text-3xl font-semibold">Com um suporte humanizado 24 horas por dia.</h3>
                    </div>
                    <div className="flex flex-col 2xl:flex-row gap-4 justify-center items-stretch">
                        <div className="flex flex-col md:flex-row gap-4 justify-center items-stretch max-[384px]:px-0 max-[480px]:px-5 px-15 sm:px-25 md:px-0">
                            <div className="flex flex-col w-auto md:w-55 xl:w-70 2xl:w-65 p-6 bg-white gap-4 rounded-2xl">
                                <img src="" alt="Ícone" className="lg:size-38px xl:size-48px aspect-square object-contain" draggable="false"></img>
                                <div>
                                    <h2 className="text-lg font-semibold">Renda Fixa</h2>
                                    <p>Investimentos para quem busca rentabilidade e segurança. Escolha o ideal para você.</p>
                                </div>
                            </div>
                            <div className="flex flex-col w-auto md:w-55 xl:w-70 2xl:w-65 p-6 bg-white gap-4 rounded-2xl">
                                <img src="" alt="Ícone" className="lg:size-38px xl:size-48px aspect-square object-contain" draggable="false"></img>
                                <div>
                                    <h2 className="text-lg font-semibold">Renda Variável</h2>
                                    <p>Oportunidades com maior potencial de retorno, ideais para quem aceita assumir mais riscos.</p>
                                </div>
                            </div>
                            <div className="flex flex-col w-auto md:w-55 xl:w-70 2xl:w-65 p-6 bg-white gap-4 rounded-2xl">
                                <img src="" alt="Ícone" className="lg:size-38px xl:size-48px aspect-square object-contain" draggable="false"></img>
                                <div>
                                    <h2 className="text-lg font-semibold">Fundos de Investimento</h2>
                                    <p>Invista de forma coletiva com gestão profissional e diversificação em diferentes ativos.</p>
                                </div>
                            </div>
                        </div>
                        <div className="flex flex-col md:flex-row gap-4 justify-center items-stretch max-[384px]:px-0 max-[480px]:px-5 px-15 sm:px-25 md:px-0">
                            <div className="flex flex-col w-auto md:w-55 xl:w-70 2xl:w-65 p-6 bg-white gap-4 rounded-2xl">
                                <img src="" alt="Ícone" className="lg:size-38px xl:size-48px aspect-square object-contain" draggable="false"></img>
                                <div>
                                    <h2 className="text-lg font-semibold">Previdência</h2>
                                    <p>Planejamento financeiro de longo prazo para garantir tranquilidade no futuro.</p>
                                </div>
                            </div>
                            <div className="flex flex-col w-auto md:w-55 xl:w-70 2xl:w-65 p-6 bg-white gap-4 rounded-2xl">
                                <img src="" alt="Ícone" className="lg:size-38px xl:size-48px aspect-square object-contain" draggable="false"></img>
                                <div>
                                    <h2 className="text-lg font-semibold">Poupança</h2>
                                    <p>A forma mais simples e segura de guardar dinheiro e manter sua reserva financeira.</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
                <section className="px-5 py-24 flex flex-col items-center justify-center gap-5">
                    <h2 className="text-3xl md:text-4xl font-bold mb-6">Conheça nossos produtos</h2>
                    <div className="flex flex-row gap-8 justify-center">
                        <a onClick={() => setActiveTab("personal")}>Para você</a>
                        <a onClick={() => setActiveTab("business")}>Para sua empresa</a>
                    </div>
                    <div className="grid md:grid-cols-3 gap-8 max-w-5xl">
                        {activeTab === "personal" && (
                            <>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Empréstimos e Financiamentos</h3>
                                </div>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Limite Garantido</h3>
                                </div>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Porquinho</h3>
                                </div>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Cartão de Crédito</h3>
                                </div>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Investimentos</h3>
                                </div>
                            </>
                        )}

                        {activeTab === "business" && (
                            <>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Empréstimos e Financiamentos</h3>
                                </div>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Maquininha</h3>
                                </div>
                                <div className="p-6 bg-blue-50 rounded-2xl shadow text-center">
                                <h3 className="font-semibold text-lg">Investimentos</h3>
                                </div>
                            </>
                        )}
                    </div>
                </section>
                <section className="px-5 py-24 bg-blue-600 text-white text-center gap-5">
                    <h2 className="text-3xl md:text-4xl font-bold mb-6">Pronto para simplificar sua vida financeira?</h2>
                    <p className="text-lg text-blue-100 mb-8">
                        Junte-se a milhares de usuários que já estão economizando tempo e dinheiro com nosso banco digital.
                    </p>
                    <Link href="/register" className="cursor-pointer">
                        <button className="px-8 py-4 bg-white text-blue-600 font-semibold rounded-2xl shadow-lg hover:bg-gray-100 transition cursor-pointer">
                            Criar minha conta agora
                        </button>
                    </Link>
                </section>
            </main>
            <PublicFooter />
        </div>
    )
}