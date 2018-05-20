$countries = @("AD", "AR", "AS", "AT", "AU", "AX", "BD", "BE", "BG", "BM", "BR", "BY", "CA", "CH", "CO", "CR", "CZ",
 "DE", "DK", "DO", "DZ", "ES", "FI", "FM", "FO", "FR", "GB", "GF", "GG", "GL", "GP", "GT", "GU", "HR", "HU", "IE", 
 "IM", "IN", "IS", "IT", "JE", "JP", "LI", "LK", "LT", "LU", "LV", "MC", "MD", "MH", "MK", "MP", "MQ", "MT", "MX", "MY", "NC", 
 "NL", "NO", "NZ", "PH", "PK", "PL", "PM", "PR", "PT", "PW", "RE", "RO", "RU", "SE", "SI", "SJ", "SK", "SM", "TH", "TR", "UA", "US",
 "UY", "VA", "VI", "WF", "YT", "ZA");

 foreach ($ctry in $countries) {
    New-Item -Path .\$($ctry.ToLower()) -Force -ItemType Directory
	(Get-Content .\pom.xml.template).replace('${code.upper}', $ctry).replace('${code.lower}', $ctry.ToLower()) | Set-Content .\$($ctry.ToLower())\pom.xml -Force
}