Feature: Order orange juice and extra hot drinks
  Scenario: Order an orange juice
    When the customer send the command "O::" with 0.6 €
    Then the drink maker returns 1 orange juice
  Scenario: Order an extra hot chocolate
    When the customer send the command "H::" with 0.5 €
    Then the drink maker returns 1 extra hot chocolate with no sugar and therefore no stick
  Scenario: Order an extra hot coffee
    When the customer send the command "C:2:0" with 0.6 €
    Then the drink maker returns 1 extra hot coffee with 2 sugars and a stick
  Scenario: Order an extra hot tea
    When the customer send the command "C:2:0" with 0.4 €
    Then the drink maker returns 1 extra hot coffee with 2 sugars and a stick
    And the drink maker returns 0.4 €
