import Navbar from "@/_components/Navbar"
import HomeHeader from "@/_components/HomeHeader"
import "../globals.css"

export default function PrivateLayout({children}: Readonly<{children: React.ReactNode}>) {
    return (
        <div className="flex flex-col w-full h-screen">
            <HomeHeader/>
            <div className="w-full h-full flex flex-col-reverse md:flex-row justify-between">
                <Navbar/>
                {children}
            </div>
        </div>
    )
}
