"use client"

import { useEffect, useState } from "react"
import { User } from "@/_lib/types"

export default function Settings() {
    const [user, setUser] = useState<User | null>(null)
    const [loading, setLoading] = useState(true)
    const [form, setForm] = useState({ fullName: "", email: "", phone: "" })
    const [avatarFile, setAvatarFile] = useState<File | null>(null)
    const [isEditing, setIsEditing] = useState(false)

    useEffect(() => {
        async function fetchUser() {
            const res = await fetch("/api/user/settings")
            const data = await res.json()
            setUser(data)
            setForm({ fullName: data.fullName, email: data.email, phone: data.phone })
            setLoading(false)
        }
        fetchUser()
    }, [])

    function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
        setForm({ ...form, [e.target.name]: e.target.value })
    }

    function handleAvatarChange(e: React.ChangeEvent<HTMLInputElement>) {
        if (e.target.files && e.target.files[0]) {
            setAvatarFile(e.target.files[0])
        }
    }

    async function handleSave() {
        let avatarUrl = user?.avatarUrl

        if (avatarFile) {
            const formData = new FormData()
            formData.append("file", avatarFile)

            const uploadRes = await fetch("/api/user/upload-avatar", {
                method: "POST",
                body: formData,
            })
            const uploadData = await uploadRes.json()
            avatarUrl = uploadData.url
        }

        const res = await fetch("/api/user/settings", {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ ...form, avatarUrl }),
        })

        const updatedUser = await res.json()
        setUser(updatedUser)
        setIsEditing(false)
    }

    return (
        <div className="flex flex-col p-10 size-full gap-4">
            <div className="flex items-center justify-start">
                <div className="flex flex-row gap-4 justify-center items-center">
                    <div className="relative size-20">
                        <img src={ avatarFile ? URL.createObjectURL(avatarFile) : user?.avatarUrl || "/default-avatar.png"} alt="Avatar" className={`size-18 rounded-full object-cover ${isEditing ? "brightness-75" : ""}`} draggable="false"></img>
                        {isEditing && (
                            <input type="file" accept="image/*" onChange={handleAvatarChange} className="absolute inset-0 opacity-0 cursor-pointer"/>
                        )}
                    </div>
                    <div className="flex flex-col items-start justify-center">
                        <span className="text-2xl font-bold leading-tight">{user?.fullName || "Nome do usuário"}</span>
                        <p className="text-lg font-semibold leading-tight">{user?.email || "Email do usuário"}</p>
                    </div>
                </div>
            </div>

            <div className="flex flex-row items-center justify-start gap-4">
                <div className="flex flex-row items-center gap-2">
                    <span>Email</span>
                    {isEditing ? (
                        <input
                            type="email"
                            name="email"
                            value={form.email}
                            onChange={handleChange}
                            className="outline-none border-b"
                        />
                    ) : (
                        <div className="flex items-start justify-center">
                            <p className="font-semibold">
                                {user?.email || "Email do usuário"}
                            </p>
                        </div>
                    )}
                </div>
                <div className="flex flex-row items-center gap-2">
                    <span>Telefone:</span>
                    {isEditing ? (
                        <input
                            type="tel"
                            name="phone"
                            value={form.phone}
                            onChange={handleChange}
                            className="outline-none border-b"
                        />
                    ) : (
                        <div className="flex items-start justify-center">
                            <p className="font-medium">
                                {user?.phone || "Telefone do usuário"}
                            </p>
                        </div>
                    )}
                </div>
            </div>

            <div className="flex gap-4">
                {isEditing ? (
                    <>
                        <button
                            onClick={handleSave}
                            className="bg-blue-500 text-white py-2 px-8 rounded hover:bg-blue-600 transition"
                        >
                            Salvar
                        </button>
                        <button
                            onClick={() => setIsEditing(false)}
                            className="bg-gray-300 py-2 px-8 rounded hover:bg-gray-400 transition"
                        >
                            Cancelar
                        </button>
                    </>
                ) : (
                    <button
                        onClick={() => setIsEditing(true)}
                        className="bg-blue-500 text-white py-2 px-8 rounded hover:bg-blue-600 transition"
                    >
                        Editar Perfil
                    </button>
                )}
            </div>
        </div>
    )
}