"use client"

import { useEffect, useState, useRef } from "react"
import { Piggy } from "@/_lib/db"

export default function Piggys() {
    const [piggys, setPiggys] = useState<Piggy[]>([])
    const [loading, setLoading] = useState(true)
    const [modal, setModal] = useState<null | "create" | "reserve" | "withdraw">(null)
    const [selectedPiggy, setSelectedPiggy] = useState<Piggy | null>(null)
    const [amount, setAmount] = useState<number>(0)
    const [name, setName] = useState("")
    const [goal, setGoal] = useState<number | undefined>(undefined)
    const modalRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        async function fetchPiggys() {
            try {
                const res = await fetch("/api/user/piggys", {
                    credentials: "include",
                })
                if (res.ok) {
                    const data = await res.json()
                    setPiggys(data)
                } else {
                    console.error("Erro ao buscar piggys")
                }
            } catch (err) {
                console.error(err)
            } finally {
                setLoading(false)
            }
        }
        fetchPiggys()
    }, [])

    async function handleCreate() {
        if (!name) return

        const res = await fetch("/api/user/piggys", {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, goal }),
        })

        if (res.ok) {
            const newPiggy = await res.json()
            setPiggys((prev) => [...prev, newPiggy])
            setModal(null)
            setName("")
            setGoal(undefined)
        } else {
            console.error("Erro ao criar porquinho")
        }
    }

    function openModal(type: "create" | "reserve" | "withdraw", piggy?: Piggy) {
        setSelectedPiggy(piggy || null)
        setAmount(0)
        setName("")
        setGoal(undefined)
        setModal(type)
    }

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (modalRef.current && !modalRef.current.contains(event.target as Node)) {
                setModal(null)
            }
        }

        document.addEventListener("mousedown", handleClickOutside)
        return () => {
            document.removeEventListener("mousedown", handleClickOutside)
        }
    }, [])

    return (
        <div className="p-10 flex flex-col size-full">
            <div>
                <h2 className="text-2xl font-semibold">Total reservado</h2>
                <span className="text-4xl font-bold">
                R$ {piggys
                        .reduce((acc, piggy) => acc + piggy.value, 0)
                        .toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
            </div>
            <div className="flex flex-row gap-4 items-center justify-cente py-4">
                <button onClick={() => openModal("create")} className="text-blue-500 font-semibold border border-blue-500 py-2 px-8 rounded-md hover:bg-blue-100 transition cursor-pointer">
                    + Criar
                </button>
                <button onClick={() => openModal("reserve")} className="text-blue-500 font-semibold border border-blue-500 py-2 px-8 rounded-md hover:bg-blue-100 transition cursor-pointer">
                    Reservar
                </button>
                <button onClick={() => openModal("withdraw")} className="text-blue-500 font-semibold border border-blue-500 py-2 px-8 rounded-md hover:bg-blue-100 transition cursor-pointer">
                    Retirar
                </button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                <h2 className="text-2xl font-semibold">Seus porquinhos</h2>
                {loading ? (
                    <p className="text-gray-500 col-span-full">Carregando...</p>
                ) : piggys.length === 0 ? (
                    <p className="text-gray-500 col-span-full">Você ainda não criou nenhum porquinho.</p>
                ) : (
                    piggys.map((piggy) => {
                        const progress = piggy.goal ? Math.min((piggy.value / piggy.goal) * 100, 100) : 0

                        return (
                            <div key={piggy.id} className="bg-white rounded-xl shadow-md p-4 flex flex-col justify-between hover:shadow-lg transition">
                                <h2 className="text-lg font-semibold mb-2">{piggy.name}</h2>
                                <p className="text-gray-600 mb-2">
                                    R$ {piggy.value.toFixed(2)} {piggy.goal ? `/ R$ ${piggy.goal.toFixed(2)}` : ""}
                                </p>

                                {piggy.goal && (
                                    <div className="w-full bg-gray-200 rounded-full h-2 mb-2">
                                        <div className="bg-blue-500 h-2 rounded-full" style={{ width: `${progress}%` }}/>
                                    </div>
                                )}

                                <button className="mt-3 bg-blue-500 text-white py-2 rounded-md hover:bg-blue-600 transition">
                                    Ver detalhes
                                </button>
                            </div>
                        )
                    })
                )}
            </div>

            {modal && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 transition-all">
                    <div ref={modalRef} className="bg-white p-6 rounded-lg w-80">
                        {modal === "create" && (
                            <>
                                <h3 className="text-xl font-semibold mb-4">Criar novo porquinho</h3>
                                <input
                                type="text"
                                placeholder="Nome do porquinho"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full border p-2 rounded mb-2"
                                />
                                <input
                                type="number"
                                placeholder="Meta (opcional)"
                                value={goal ?? ""}
                                onChange={(e) => setGoal(Number(e.target.value))}
                                className="w-full border p-2 rounded mb-4"
                                />
                                <div className="flex justify-end gap-2">
                                    <button onClick={() => setModal(null)} className="py-2 px-4 rounded border hover:bg-gray-100 transition">
                                        Cancelar
                                    </button>
                                    <button onClick={handleCreate} className="py-2 px-4 rounded bg-blue-500 text-white hover:bg-blue-600 transition">
                                        Criar
                                    </button>
                                </div>
                            </>
                        )}

                        {(modal === "reserve" || modal === "withdraw") && selectedPiggy && (
                            <>
                                <h3 className="text-lg font-semibold mb-4">
                                {modal === "reserve" ? "Reservar" : "Retirar"} no {selectedPiggy.name}
                                    </h3>
                                    <input
                                    type="number"
                                    min={0}
                                    value={amount}
                                    onChange={(e) => setAmount(Number(e.target.value))}
                                    placeholder="Digite o valor"
                                    className="w-full border p-2 rounded mb-4"
                                />
                                <div className="flex justify-end gap-2">
                                    <button onClick={() => setModal(null)} className="py-2 px-4 rounded border hover:bg-gray-100 transition">
                                        Cancelar
                                    </button>
                                    <button className="py-2 px-4 rounded bg-blue-500 text-white hover:bg-blue-600 transition">
                                        Confirmar
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    )
}