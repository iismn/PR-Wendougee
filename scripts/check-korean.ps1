$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$count = 0
$modules = 0
$placeholderPattern = '%(?:\d+\$)?[dsf]'

function Get-Entries($document) {
    $entries = @{}
    foreach ($node in $document.resources.ChildNodes) {
        if ($node.NodeType -ne [System.Xml.XmlNodeType]::Element) { continue }
        $key = $node.GetAttribute('name')
        if ($entries.ContainsKey($key)) { throw "Duplicate resource: $key" }
        $entries[$key] = $node
    }
    return $entries
}

function Check-Placeholders($source, $translation, $key) {
    $expected = @([regex]::Matches($source, $placeholderPattern) | ForEach-Object Value | Sort-Object) -join ','
    $actual = @([regex]::Matches($translation, $placeholderPattern) | ForEach-Object Value | Sort-Object) -join ','
    if ($expected -cne $actual) { throw "Placeholder mismatch in ${key}: $expected / $actual" }
}

Get-ChildItem -LiteralPath (Join-Path $repo 'shared') -Filter strings.xml -Recurse |
    Where-Object { $_.Directory.Name -eq 'values' -and $_.Directory.Parent.Name -eq 'composeResources' } |
    ForEach-Object {
        $source = Get-Entries ([xml](Get-Content -LiteralPath $_.FullName -Raw -Encoding utf8))
        if ($source.Count -eq 0) { return }
        $translationPath = Join-Path $_.Directory.Parent.FullName 'values-ko/strings.xml'
        if (-not (Test-Path -LiteralPath $translationPath)) { throw "Missing Korean module: $translationPath" }
        $translated = Get-Entries ([xml](Get-Content -LiteralPath $translationPath -Raw -Encoding utf8))
        if ($source.Count -ne $translated.Count) { throw "Resource count mismatch: $translationPath" }
        foreach ($key in $source.Keys) {
            if (-not $translated.ContainsKey($key)) { throw "Missing Korean resource: $key" }
            $original = $source[$key]
            $korean = $translated[$key]
            if ($original.LocalName -ne $korean.LocalName) { throw "Resource type mismatch: $key" }
            if ($original.LocalName -eq 'plurals') {
                $other = $original.SelectSingleNode('item[@quantity="other"]')
                if (-not $korean.SelectSingleNode('item[@quantity="other"]')) { throw "Missing Korean plural fallback: $key" }
                foreach ($item in $korean.item) { Check-Placeholders $other.InnerText $item.InnerText $key }
            } else {
                Check-Placeholders $original.InnerText $korean.InnerText $key
            }
        }
        $count += $source.Count
        $modules++
    }
Write-Output "Korean resources verified: $count entries in $modules modules; XML, keys, types and placeholders match."

$ios = [xml](Get-Content (Join-Path $repo 'iosApp/iosApp/Info.plist') -Raw -Encoding utf8)
if (-not $ios.SelectSingleNode('//key[.="CFBundleLocalizations"]/following-sibling::array[1]/string[.="ko"]')) {
    throw 'Korean is missing from iOS localizations'
}
$android = [xml](Get-Content (Join-Path $repo 'androidApp/src/main/res/xml/locales_config.xml') -Raw -Encoding utf8)
if (-not ($android.'locale-config'.locale | Where-Object { $_.GetAttribute('name', 'http://schemas.android.com/apk/res/android') -eq 'ko' })) {
    throw 'Korean is missing from Android locales'
}
Write-Output 'iOS and Android Korean language registration verified.'
