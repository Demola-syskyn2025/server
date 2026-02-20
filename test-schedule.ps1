# Use V2 credentials for Docker (dr.sarah.johnson), or DataSeeder credentials for H2 (dr.smith)
$body = @{ email = "dr.sarah.johnson@hospital.com"; password = "password" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method Post -ContentType "application/json" -Body $body
$token = $login.token

$today = Get-Date
$dow = [int]$today.DayOfWeek
if ($dow -eq 0) { $daysToAdd = 1 } else { $daysToAdd = 8 - $dow }
$nextMonday = $today.AddDays($daysToAdd).ToString("yyyy-MM-dd")
Write-Host "Generating plan for: $nextMonday"

$planBody = @{ weekStartDate = $nextMonday } | ConvertTo-Json
try {
    $result = Invoke-RestMethod -Uri http://localhost:8080/api/schedule-plans/generate -Method Post -ContentType "application/json" -Headers @{Authorization="Bearer $token"} -Body $planBody
    
    Write-Host "`n=== Plan ID: $($result.id), Status: $($result.status) ==="
    
    # Get summary
    $summary = Invoke-RestMethod -Uri "http://localhost:8080/api/schedule-plans/$($result.id)/summary" -Method Get -Headers @{Authorization="Bearer $token"}
    
    Write-Host "`n=== Staff Breakdown ==="
    foreach ($staff in $summary.staffSummaries) {
        Write-Host "$($staff.staffName): visits=$($staff.totalVisits), office=$($staff.totalOfficeBlocks), totalMin=$($staff.totalWorkMinutes) ($([math]::Round($staff.totalWorkMinutes/60,1))h), dayOff=$($staff.dayOff)"
    }
    
    Write-Host "`n=== Violations ($($result.violations.Count)) ==="
    foreach ($v in $result.violations) { Write-Host "  $v" }
    
    Write-Host "`n=== Total visits: $($summary.totalVisits), Total office: $($summary.totalOfficeBlocks) ==="
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host $reader.ReadToEnd()
}
