import { NextResponse } from "next/server"

export async function POST(req: Request) {
    const { email } = await req.json()

    if (!email) {
        return NextResponse.json({ error: "E-mail é obrigatório" }, { status: 400 })
    }

    console.log("Enviar email de recuperação para:", email)

    return NextResponse.json({ message: "Se este e-mail estiver cadastrado, enviaremos um link de recuperação." })
}