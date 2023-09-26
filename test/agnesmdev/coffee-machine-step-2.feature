Feature: Order a drink and pay for it
  Scenario: Order a tea
    When the customer send the command "T:1:0" with 0.4 €
    Then the drink maker returns 1 tea with 1 sugar and a stick
  Scenario: Order a chocolate
    When the customer send the command "H::" with 0.5 €
    Then the drink maker returns 1 chocolate with no sugar and therefore no stick
  Scenario: Order a coffee
    When the customer send the command "C:2:0" with 0.6 €
    Then the drink maker returns 1 coffee with 2 sugars and a stick
  Scenario: Order a coffee with too much money
    When the customer send the command "C:2:0" with 1 €
    Then the drink maker returns 1 coffee with 2 sugars and a stick
    And the drink maker returns 0.4 €
  Scenario: Order a tea without enough money
    When the customer send the command "T:1:0" with 0.3 €
    Then the drink maker forwards "Not enough money, missing 0.1 €" onto the coffee machine interface
