Feature: Patient CRUD Operations

  Background:
    * url baseUrl
    * def randomEmail = 'patient_' + java.util.UUID.randomUUID() + '@mail.com'

  Scenario: Full CRUD Patient

    # Create
    Given path '/v1/patients'
    And request
    """
    {
      "firstName": "Jane",
      "lastName": "Doe",
      "email": "#(randomEmail)",
      "address": "Main Street",
      "dateOfBirth": "1990-01-01"
    }
    """
    When method POST
    Then status 201
    * def patientId = response.id

    # Retrieve
    Given path '/v1/patients', patientId
    When method GET
    Then status 200
    And match response.id == patientId

    # List
    Given path '/v1/patients'
    When method GET
    Then status 200
    And match response.content[*].id contains patientId

    # Update
    Given path '/v1/patients', patientId
    And request
    """
    {
      "firstName": "Jane Updated",
      "lastName": "Doe",
      "email": "#(randomEmail)",
      "address": "Updated Street",
      "dateOfBirth": "1990-01-01"
    }
    """
    When method PUT
    Then status 200

    # Delete
    Given path '/v1/patients', patientId
    When method DELETE
    Then status 204