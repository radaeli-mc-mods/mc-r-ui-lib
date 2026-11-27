param(
    [string]$Forge = "$PSScriptRoot/../forge/runs/validation/ui-validation",
    [string]$NeoForge = "$PSScriptRoot/../neoforge/runs/validation/ui-validation",
    [string]$Output = "$PSScriptRoot/../build/reports/ui-parity.json"
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$drawingReferences=if ($PSVersionTable.PSEdition -eq 'Core') { @('System.Drawing.Common','System.Drawing.Primitives','System.Runtime','System.Runtime.InteropServices') } else { @('System.Drawing') }
Add-Type -ReferencedAssemblies $drawingReferences -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public static class UiPixels {
    public static int Different(Bitmap a, Bitmap b) {
        var rect = new Rectangle(0, 0, a.Width, a.Height);
        var ad = a.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        var bd = b.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        try {
            int different = 0;
            int[] ar = new int[a.Width], br = new int[b.Width];
            for (int y = 0; y < a.Height; y++) {
                Marshal.Copy(IntPtr.Add(ad.Scan0, y * ad.Stride), ar, 0, ar.Length);
                Marshal.Copy(IntPtr.Add(bd.Scan0, y * bd.Stride), br, 0, br.Length);
                for (int x = 0; x < ar.Length; x++) if (ar[x] != br[x]) different++;
            }
            return different;
        } finally { a.UnlockBits(ad); b.UnlockBits(bd); }
    }
}
'@
$forgeResult = Get-Content -LiteralPath "$Forge/results.json" -Raw | ConvertFrom-Json
$neoResult = Get-Content -LiteralPath "$NeoForge/results.json" -Raw | ConvertFrom-Json
if (!$forgeResult.passed -or !$neoResult.passed) { throw 'Both runtime suites must pass.' }
if (@(Compare-Object $forgeResult.checks $neoResult.checks).Count) { throw 'The loaders ran different checks.' }
if (@(Compare-Object $forgeResult.captures $neoResult.captures).Count) { throw 'The loaders produced different captures.' }
$results = @()
foreach ($name in $forgeResult.captures) {
    $a = [System.Drawing.Bitmap]::new((Resolve-Path "$Forge/$name.png").Path)
    $b = [System.Drawing.Bitmap]::new((Resolve-Path "$NeoForge/$name.png").Path)
    try {
        if ($a.Width -ne $b.Width -or $a.Height -ne $b.Height) { throw "Capture dimensions differ: $name" }
        $different = [UiPixels]::Different($a, $b)
        $results += [pscustomobject]@{capture=$name; differentPixels=$different; totalPixels=$a.Width*$a.Height; identical=($different -eq 0)}
    } finally { $a.Dispose(); $b.Dispose() }
}
New-Item -ItemType Directory -Force (Split-Path $Output) | Out-Null
$results | ConvertTo-Json | Set-Content -LiteralPath $Output
$results | Format-Table
foreach ($folder in @($Forge, $NeoForge)) {
    $cursor0 = [System.Drawing.Bitmap]::new((Resolve-Path "$folder/cursor-0.png").Path)
    $cursor1 = [System.Drawing.Bitmap]::new((Resolve-Path "$folder/cursor-1.png").Path)
    $cursor2 = [System.Drawing.Bitmap]::new((Resolve-Path "$folder/cursor-2.png").Path)
    try {
        if ([UiPixels]::Different($cursor0, $cursor1) -eq 0 -or [UiPixels]::Different($cursor0, $cursor2) -ne 0) {
            throw "Cursor does not blink with the expected 300 ms interval: $folder"
        }
    } finally { $cursor0.Dispose(); $cursor1.Dispose(); $cursor2.Dispose() }
}
if (@($results | Where-Object { !$_.identical }).Count) { throw 'Visual differences found; inspect captures before accepting parity.' }
