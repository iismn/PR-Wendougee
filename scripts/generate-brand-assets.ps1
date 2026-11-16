# Rasterize the official circular symbol using the same 100-unit geometry as Logo.kt.
# Run on Windows; generated assets are checked in for Xcode and desktop packaging.
$brandRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$brandOutput = Join-Path $brandRoot 'build/brand-icons'
New-Item -ItemType Directory -Force -Path $brandOutput | Out-Null
Add-Type -AssemblyName System.Drawing
$brandBackground = [System.Drawing.Color]::FromArgb(16, 26, 43)
$brandBrush = [System.Drawing.SolidBrush]::new($brandBackground)
$brandSizes = @(16, 32, 48, 64, 128, 256, 512, 1024)
foreach ($brandSize in $brandSizes) {
    $bitmap = [System.Drawing.Bitmap]::new($brandSize, $brandSize)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear($brandBackground)
    $scale = $brandSize / 160.0
    $graphics.FillEllipse([System.Drawing.Brushes]::White,
        [single](30 * $scale), [single](30 * $scale), [single](100 * $scale), [single](100 * $scale))
    $graphics.FillEllipse($brandBrush,
        [single](53.65 * $scale), [single](53.65 * $scale), [single](52.7 * $scale), [single](52.7 * $scale))
    $graphics.FillEllipse($brandBrush,
        [single](72.75 * $scale), [single](34.55 * $scale), [single](14.5 * $scale), [single](14.5 * $scale))
    $bitmap.Save((Join-Path $brandOutput "icon-$brandSize.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}
$brandBrush.Dispose()
Copy-Item -LiteralPath (Join-Path $brandOutput 'icon-1024.png') -Destination (Join-Path $brandRoot 'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png')
Copy-Item -LiteralPath (Join-Path $brandOutput 'icon-256.png') -Destination (Join-Path $brandRoot 'desktopApp/src/main/resources/icon.png')

$ico = [System.IO.MemoryStream]::new()
$writer = [System.IO.BinaryWriter]::new($ico)
$icoSizes = @(16, 32, 48, 64, 128, 256)
$writer.Write([uint16]0)
$writer.Write([uint16]1)
$writer.Write([uint16]$icoSizes.Count)
$offset = 6 + 16 * $icoSizes.Count
foreach ($size in $icoSizes) {
    $png = [System.IO.File]::ReadAllBytes((Join-Path $brandOutput "icon-$size.png"))
    $dimension = if ($size -eq 256) { 0 } else { $size }
    $writer.Write([byte]$dimension)
    $writer.Write([byte]$dimension)
    $writer.Write([uint16]0)
    $writer.Write([uint16]1)
    $writer.Write([uint16]32)
    $writer.Write([uint32]$png.Length)
    $writer.Write([uint32]$offset)
    $offset += $png.Length
}
foreach ($size in $icoSizes) { $writer.Write([System.IO.File]::ReadAllBytes((Join-Path $brandOutput "icon-$size.png"))) }
[System.IO.File]::WriteAllBytes((Join-Path $brandRoot 'desktopApp/icon.ico'), $ico.ToArray())
$writer.Dispose()
$ico.Dispose()

function Write-BrandBigEndian([System.IO.Stream]$stream, [uint32]$number) {
    $bytes = [BitConverter]::GetBytes($number)
    if ([BitConverter]::IsLittleEndian) { [Array]::Reverse($bytes) }
    $stream.Write($bytes, 0, 4)
}
$icns = [System.IO.MemoryStream]::new()
$chunks = [ordered]@{ icp4 = 16; icp5 = 32; icp6 = 64; ic07 = 128; ic08 = 256; ic09 = 512; ic10 = 1024 }
$icns.Write([Text.Encoding]::ASCII.GetBytes('icns'), 0, 4)
Write-BrandBigEndian $icns 0
foreach ($chunk in $chunks.GetEnumerator()) {
    $png = [System.IO.File]::ReadAllBytes((Join-Path $brandOutput ("icon-" + $chunk.Value + '.png')))
    $icns.Write([Text.Encoding]::ASCII.GetBytes($chunk.Key), 0, 4)
    Write-BrandBigEndian $icns ($png.Length + 8)
    $icns.Write($png, 0, $png.Length)
}
$icns.Position = 4
Write-BrandBigEndian $icns $icns.Length
[System.IO.File]::WriteAllBytes((Join-Path $brandRoot 'desktopApp/icon.icns'), $icns.ToArray())
$icns.Dispose()
Write-Output 'Generated Wendougee iOS, Windows, and macOS launcher assets.'
