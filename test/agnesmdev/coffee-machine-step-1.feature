Feature: Order a drink
  Scenario: Order a tea
    When the customer send the command "T:1:0"
    Then the drink maker returns 1 tea with 1 sugar and a stick
  Scenario: Order a chocolate
    When the customer send the command "H::"
    Then the drink maker returns 1 chocolate with no sugar and therefore no stick
  Scenario: Order a coffee
    When the customer send the command "C:2:0"
    Then the drink maker returns 1 coffee with 2 sugars and a stick
  Scenario: Send a message
    When the customer send the command "M:message-content"
    Then the drink maker forwards "message-content" onto the coffee machine interface
