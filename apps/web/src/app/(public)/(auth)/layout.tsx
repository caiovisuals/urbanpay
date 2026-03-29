import AuthHeader from "@/_components/AuthHeader"

export default function AuthLayout({children}: Readonly<{children: React.ReactNode}>) {
    return (
        <div className="flex flex-col bg-gray-900 text-[var(--white-text)]">
            <AuthHeader />
            <div className="h-[calc(100vh-92px)]">
                {children}
            </div>
        </div>
    )
}

