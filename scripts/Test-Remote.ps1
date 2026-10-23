param([Parameter(Mandatory = $true)][ValidatePattern('^[a-z]{20}$')][string]$ProjectRef)
$ErrorActionPreference = 'Stop'
$endpoint = "https://$ProjectRef.supabase.co/functions/v1/search-products"
$response = Invoke-RestMethod -Uri $endpoint -Method Post -ContentType 'application/json' -Body (@{ query = 'Nintendo Switch OLED' } | ConvertTo-Json) -TimeoutSec 25
if ($null -eq $response.offers) { throw 'El backend no devolvió el contrato offers.' }
Write-Output "Integración remota correcta: $($response.offers.Count) resultados."
$response | ConvertTo-Json -Depth 6
