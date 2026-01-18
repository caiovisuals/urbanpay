import "./globals.css"
import type { Metadata } from "next"

export const metadata: Metadata = {
    title: "Urbanpay - O seu Banco e Gateway de Pagamento Digital",
    description: "O banco digital feito pra você! Pagamento rápido e fácil para receber e transacionar via Pix, cartão de crédito, boleto e muito mais.",
    metadataBase: new URL("https://urbanpay.com"),
    keywords: [
        "urbanpay", "fintech", "caiovisuals"
    ],
    openGraph: {
        type: "website",
        locale: "pt_BR",
        images: [{
            url: "https://www.urbanpay.com/ogimage.png",
            width: 2400,
            height: 1260,
            alt: "Preview do Urbanpay",
            type: "image/png"
        }],
        countryName: "Brasil",
    },
    twitter: {
        card: "summary_large_image",
        site: "@caioba2007",
        images: ["https://urbanpay.com/ogimage.png"],
    },
    robots: {
        index: true,
        follow: true,
        googleBot: {
            index: true,
            follow: true,
            "max-image-preview": "large",
        },
    },
    alternates: {
        languages: {
            'pt-BR': '/'
        }
    },
    other: {
        "google": "notranslate",
        "application-name": "URBANPAY",
    }
}

export default function PrincipalLayout({children,}: Readonly<{children: React.ReactNode;}>) {
    return (
        <html lang="pt-br">
            <body className="size-full">
                {children}
            </body>
        </html>
    )
}

