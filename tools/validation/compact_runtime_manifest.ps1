param(
    [Parameter(Mandatory = $true)]
    [string] $Capture,

    [Parameter(Mandatory = $true)]
    [string] $Output
)

$runtime = Get-Content -LiteralPath $Capture -Raw | ConvertFrom-Json
$blocks = @($runtime.blocks.id | Sort-Object -Unique)
$items = @($runtime.items.id | Sort-Object -Unique)
$fluids = @($runtime.fluids | ForEach-Object { "basemetals:$($_.name)" } | Sort-Object -Unique)
$stateCount = ($runtime.blocks.states | Measure-Object).Count

$manifest = [ordered]@{
    format = 2
    source = 'Live Forge 1.12.2 registry and saved-world capture'
    minecraft = '1.12.2'
    forge = '14.23.5.2847'
    base_metals_artifact = 'BaseMetals-1.12-2.5.0-rc2.332.jar'
    base_metals_sha256 = '034FD791A9E77345C19F6098C01D4B5B66F90EF4A953FB0831089935EAD9DFFB'
    mmdlib_artifact = 'MMDLib-1.12-1.0.0-rc2.36.jar'
    mmdlib_sha256 = 'FE2229E6755A5FD306C33CC22DCCF055378D371339133E4912C5DC9213E360AF'
    block_state_count = $stateCount
    blocks = $blocks
    items = $items
    fluids = $fluids
}

$parent = Split-Path -Parent $Output
if ($parent) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
}
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $Output -Encoding UTF8
