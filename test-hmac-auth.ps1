# Test HMAC Authentication & Authorization (PowerShell)
# Usage: .\test-hmac-auth.ps1 -AgencyCode "AGENCY_A" -ApiKey "tvb_live_xxx" -Secret "your_secret" -TargetPath "/AGENCY_A/transactions/received"

param(
    [string]$AgencyCode = "AGENCY_A",
    [string]$ApiKey = "tvb_live_xxx",
    [string]$Secret = "your_secret_here",
    [string]$TargetPath = "/$AgencyCode/transactions/received"
)

$BaseUrl = "http://localhost:8080"
$Timestamp = [int][double]::Parse((Get-Date -UFormat %s))
$Nonce = [guid]::NewGuid().ToString()

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "HMAC Authentication Test" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Agency Code: $AgencyCode"
Write-Host "API Key: $ApiKey"
Write-Host "Target Path: $TargetPath"
Write-Host "Timestamp: $Timestamp"
Write-Host "Nonce: $Nonce"
Write-Host ""

# Calculate canonical string
$Method = "GET"
$QueryString = ""
$BodyHash = ""

$CanonicalString = "$Method`n$TargetPath`n$QueryString`n$ApiKey`n$Timestamp`n$Nonce`n$BodyHash"

Write-Host "Canonical String:" -ForegroundColor Yellow
Write-Host $CanonicalString
Write-Host ""

# Calculate HMAC signature
$hmacsha256 = New-Object System.Security.Cryptography.HMACSHA256
$hmacsha256.Key = [Text.Encoding]::UTF8.GetBytes($Secret)
$signatureBytes = $hmacsha256.ComputeHash([Text.Encoding]::UTF8.GetBytes($CanonicalString))
$Signature = [Convert]::ToBase64String($signatureBytes)

Write-Host "Signature: $Signature" -ForegroundColor Yellow
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Making Request..." -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# Make request
$headers = @{
    "X-Api-Key" = $ApiKey
    "X-Timestamp" = $Timestamp
    "X-Nonce" = $Nonce
    "X-Signature" = $Signature
}

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl$TargetPath" -Method Get -Headers $headers -ErrorAction Stop
    $statusCode = $response.StatusCode
    $body = $response.Content | ConvertFrom-Json
    
    Write-Host "HTTP Status: $statusCode" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Green
    $body | ConvertTo-Json -Depth 10
    
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "Result Analysis" -ForegroundColor Cyan
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "✅ SUCCESS - Request authorized" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $body = $_.Exception.Response.Content
    
    Write-Host "HTTP Status: $statusCode" -ForegroundColor Red
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Red
    try {
        $errorBody = $body | ConvertFrom-Json
        $errorBody | ConvertTo-Json -Depth 10
    } catch {
        Write-Host $body
    }
    
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "Result Analysis" -ForegroundColor Cyan
    Write-Host "=========================================" -ForegroundColor Cyan
    
    switch ($statusCode) {
        401 {
            Write-Host "❌ AUTHENTICATION FAILED" -ForegroundColor Red
            Write-Host "Possible causes:"
            Write-Host "  - Invalid API key"
            Write-Host "  - Invalid signature"
            Write-Host "  - Timestamp skew"
            Write-Host "  - Agency suspended"
        }
        403 {
            Write-Host "❌ AUTHORIZATION FAILED" -ForegroundColor Red
            Write-Host "Agency '$AgencyCode' (owner of API key) tried to access path: $TargetPath"
            Write-Host "This indicates cross-agency access attempt was blocked ✅" -ForegroundColor Yellow
        }
        default {
            Write-Host "❌ UNEXPECTED HTTP CODE: $statusCode" -ForegroundColor Red
        }
    }
}

# Test cross-agency access
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test Cross-Agency Access" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$CrossPath = if ($AgencyCode -eq "AGENCY_A") { "/AGENCY_B/transactions/received" } else { "/AGENCY_A/transactions/received" }
Write-Host "Attempting to access: $CrossPath" -ForegroundColor Yellow

$CrossCanonical = "$Method`n$CrossPath`n$QueryString`n$ApiKey`n$Timestamp`n$([guid]::NewGuid().ToString())`n$BodyHash"
$crossSignatureBytes = $hmacsha256.ComputeHash([Text.Encoding]::UTF8.GetBytes($CrossCanonical))
$CrossSignature = [Convert]::ToBase64String($crossSignatureBytes)

$crossHeaders = @{
    "X-Api-Key" = $ApiKey
    "X-Timestamp" = $Timestamp
    "X-Nonce" = [guid]::NewGuid().ToString()
    "X-Signature" = $CrossSignature
}

try {
    $crossResponse = Invoke-WebRequest -Uri "$BaseUrl$CrossPath" -Method Get -Headers $crossHeaders -ErrorAction Stop
    Write-Host "HTTP Status: $($crossResponse.StatusCode)" -ForegroundColor Red
    Write-Host "❌ SECURITY ISSUE - Cross-agency access should return 403" -ForegroundColor Red
} catch {
    $crossStatusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "HTTP Status: $crossStatusCode"
    
    if ($crossStatusCode -eq 403) {
        Write-Host "✅ CORRECT - Cross-agency access blocked" -ForegroundColor Green
    } else {
        Write-Host "❌ UNEXPECTED - Expected 403 but got $crossStatusCode" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
