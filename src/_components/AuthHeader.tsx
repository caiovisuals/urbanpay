"use client"

import Link from "next/link"
import Image from "next/image"

export default function AuthHeader() {
    return (
        <header className="flex justify-between items-center gap-4 px-5 lg:px-10 py-6">
            <div className="w-[20%]">
                <Link href="/" className="inline-block w-fit">
                    <div className="w-32 aspect-[2838/760] relative">
                        <Image src="/assets/white-logo.png" alt="Urb Logo" className="select-none object-contain" draggable="false" fill />
                    </div>
                </Link>
            </div>
        </header>
    )
}