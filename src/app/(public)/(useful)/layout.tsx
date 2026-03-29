import PublicHeader from "@/_components/PublicHeader"
import PublicFooter from "@/_components/PublicFooter"

export default function UsefulLayout({children}: Readonly<{children: React.ReactNode}>) {
    return (
        <div className="flex flex-col">
            <PublicHeader/>
            {children}
            <PublicFooter/>
        </div>
    )
}
