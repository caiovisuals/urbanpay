/**
 * Dinheiro no front: o core envia e recebe strings decimais ("1234.50").
 * Nada aqui usa aritmética de ponto flutuante.
 */

/** "1234.5" | "-12.30" -> "R$ 1.234,50" | "-R$ 12,30" */
export function formatBRL(value: string): string {
    const negative = value.startsWith("-")
    const [int, frac = ""] = value.replace("-", "").split(".")
    const grouped = (int || "0").replace(/^0+(?=\d)/, "").replace(/\B(?=(\d{3})+(?!\d))/g, ".")
    return `${negative ? "-" : ""}R$ ${grouped},${frac.padEnd(2, "0").slice(0, 2)}`
}

/**
 * Máscara de digitação estilo app de banco: os dígitos entram pela direita.
 * "1" -> 0,01 · "123" -> 1,23. Devolve a string decimal para a API ("1.23") ou "" se vazio.
 */
export function decimalFromTypedDigits(typed: string, maxDigits = 11): string {
    const digits = typed.replace(/\D/g, "").replace(/^0+/, "").slice(0, maxDigits)
    if (!digits) return ""
    const padded = digits.padStart(3, "0")
    return `${padded.slice(0, -2)}.${padded.slice(-2)}`
}

export function isPositiveDecimal(value: string): boolean {
    return /^\d+\.\d{2}$/.test(value) && /[1-9]/.test(value)
}
