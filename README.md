# The Asset Management Digital Challenge

## Building the application
 On command line, go to project directory and execute 'gradlew build'

## Running the application
 Execute './gradlew bootRun' after building the application.

## Transfer API
  http post  /v1/transfers

   payload : {
       "fromAccountId" : "ID-A",
       "toAccountId" : "ID-B",
       "transferAmount" : 100.00
   }

## Possible Improvements
- Replace in memory data store with DB.
- Add persistence layer
- Add updateAccount method in AccountsRepository
- Support transfer rollback
- Add security to Rest API
- Apply HATEOS to Rest API
- Add some thing like Spring sleuth to trace logs.
- Create transaction logs for tracking money transfers
- If the service is planned to scale horizontally, revisit the locking strategy.
- Add Swagger to provide api documentation
