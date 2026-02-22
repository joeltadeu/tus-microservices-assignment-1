Feature: Doctor CRUD Operations

  Background:
    * url baseUrl
    * def UUID = Java.type('java.util.UUID')
    * def randomEmail =
      """
      function() {
        return 'doctor_' + UUID.randomUUID() + '@mail.com';
      }
      """

  Scenario: Full CRUD Doctor

    # Create
    Given path '/v1/doctors'
    And request
    """
    {
      "firstName": "Mark",
      "lastName": "House",
      "title": "Dr.",
      "specialityId": 1,
      "email": "#(randomEmail())",
      "phone": "+1-555-1111",
      "department": "Diagnostics"
    }
    """
    When method POST
    Then status 201
    And match response.id != null
    * def doctorId = response.id

    # Retrieve
    Given path '/v1/doctors', doctorId
    When method GET
    Then status 200
    And match response.id == doctorId

    # List
    Given path '/v1/doctors'
    When method GET
    Then status 200
    And match response.content[*].id contains doctorId

    # Update
    Given path '/v1/doctors', doctorId
    And request
    """
    {
      "firstName": "Gregory",
      "lastName": "House",
      "title": "Dr.",
      "specialityId": 1,
      "email": "#(randomEmail())",
      "phone": "+1-555-2222",
      "department": "Diagnostics"
    }
    """
    When method PUT
    Then status 200

    # Delete
    Given path '/v1/doctors', doctorId
    When method DELETE
    Then status 204