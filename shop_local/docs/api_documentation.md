# Спецификация API системы

## Используемые DTO
```
ProductAttributesDto:
    name: string
    price: double
    number: integer

ProductDto extends ProductAttributesDto:
    id: string

ProductInOrderDto extends ProductDto

createProductRequestDto extends ProductAttributesDto:
    catalog_id: string

ProductInCatalogueDto extends ProductDto:
    number_booked: integer

Catalogue: 
    products: ProductInCatalogueDto[]

UpdateQuantityDto:
    quantity: integer

TimeslotDto:
    timeslot_start: string
    timeslot_end: string

DeliveryDto:
    id: string
    timeslot: TimeslotDto
    order: OrderDto

OrderStatusEnum: 
    'collecting' = 'COLLECTING',
    'booked' = 'BOOKED',
    'paid' = 'PAID',
    'shipping' = 'SHIPPING',
    'refund' = 'REFUND',
    'completed' = 'COMPLETED',
    'discarded' = 'DISCARDED'

CustomerDto: 
    id: string
    orders: OrderDto[]

OrderDto:
    id: string
    customerID: string
    products: ProductInOrderDto[] || []
    start_date: string
    sum: double
    delivery?: DeliveryDto
    status: OrderStatusEnum
    
NewProductInOrderDto:
    productInCatalogueID: string
    quantity: int
    
AuthenticationRequestDto:
    username: string
    password: string
AuthenticationResponseDto:
    accessToken: string 
    refreshToken: string
RegistrationRequestDto extends AuthenticationRequestDto:
    name: string,
    surname: string,
    email: string
```
## Заказы
### Создать заказ
```
REQUEST:
 HTTP VERB: POST
 URL: /orders
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | OderDto
```
### Добавить продукт в заказ
```
REQUEST:
 HTTP VERB: POST
 URL: /orders/{order_id}/products   
 HEADERS:                           
  Authorization: Bearer access_token
 PARAMETERS:
  order_id: UUID
 BODY FORMAT: NewProductInOrderDto  

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | OrderDto
```
### Удалить продукт из заказа
```
REQUEST:
 HTTP VERB: DELETE
 URL: /order/product/{product_in_order_id}
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  product_in_order_id: UUID

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
### Изменить статус заказа 
```
REQUEST:
 HTTP VERB: POST
 URL: /orders/{order_id}/status 
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  order_id: UUID
 BODY FORMAT: OrderStatusEnum
 

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: order not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
### Создать доставку заказа
```
REQUEST:
 HTTP VERB: POST
 URL: /orders/{order_id}/delivery
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  order_id: UUID

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | DeliveryDto
```
### Оплатить заказ
```
REQUEST:
 HTTP VERB: POST
 URL: /order/{order_id}/pay
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  order_id: UUID

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
### Сделать рефанд заказа
```
REQUEST:
 HTTP VERB: POST
 URL: /order/{order_id}/refund
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  order_id: UUID

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
## Пользователи
### Получить список заказов текущего пользователя
```
REQUEST:
 HTTP VERB: GET
 URL: /orders
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | OrderDto[]
```
## Аутентификация
### Зарегистрироваться
```
REQUEST:
 HTTP VERB: POST
 URL: /authentication/signup
 BODY FORMAT: RegistrationRequestDto
RESPONSE:
 HTTP CODES:
  200: success
  400: bad request
 BODY FORMAT: { error: HttpException } | {}
```
### Войти в систему
```
REQUEST:
 HTTP VERB: POST
 URL: /authentication/signin
 BODY FORMAT: AuthenticationRequestDto
RESPONSE:
 HTTP CODES:
  200: success
  404: user not found
  403: invalid password
 BODY FORMAT: { error: HttpException } | AuthenticationResponseDto
```
### Обновить токен(сессию)
```
REQUEST:
 HTTP VERB: POST
 URL: /authentication/refresh
 HEADERS:
   Authorization: Bearer access_token
RESPONSE:
 HTTP CODES:
  200: success
  403: authentication error
 BODY FORMAT: { error: HttpException } | AuthenticationResponseDto
```
## Доставка
### Получить слоты доставки
```
REQUEST:
 HTTP VERB: GET
 URL: /delivery/{delivery_id}/timeslots
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  delivery_id: UUID

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | TimeslotDto[]
```
### Назначить слот для доставки
```
REQUEST:
 HTTP VERB: POST
 URL: /delivery/{delivery_id}/timeslot/set
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  delivery_id: UUID
 BODY FORMAT: TimeslotDto

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
## Товары
### Создать товар
```
REQUEST:
 HTTP VERB: POST
 URL: /product
 HEADERS:
  Authorization: Bearer access_token
 BODY FORMAT: createProductRequestDto

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | ProductInCatalogueDto
```
### Изменить количество единиц товара
```
REQUEST:
 HTTP VERB: PATCH
 URL: /product/{product_id}/number
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  product_id: UUID
 BODY FORMAT: UpdateQuantityDto

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
### Изменить количество забронированных единиц товара
```
REQUEST:
 HTTP VERB: PATCH
 URL: /product/{product_id}/booked
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  product_id: UUID
 BODY FORMAT: UpdateQuantityDto

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
## Каталоги
### Создать каталог
```
REQUEST:
 HTTP VERB: POST
 URL: /catalogue
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | CatalogueDto
```
### Удалить товар из каталога
```
REQUEST:
 HTTP VERB: DELETE
 URL: /catalogue/{catalogue_id}/product/{product_id}
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  product_id: UUID
  catalogue_id: UUID

RESPONSE:
 HTTP CODES:
  200: success
  401: unauthorised
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | {}
```
## Получение списков
### Получить список заказов
```
REQUEST:
 HTTP VERB: GET
 URL: /orders/all
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | OrderDto[]
```
### Получить список пользователей
```
REQUEST:
 HTTP VERB: GET
 URL: /customers
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | CustomerDto[]
```
### Получить список продуктов
```
REQUEST:
 HTTP VERB: GET
 URL: /products/all
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | ProductDto[]
```
### Получить список продуктов в каталоге
```
REQUEST:
 HTTP VERB: GET
 URL: /products/catalogue/{catalogue_id}
 HEADERS:
  Authorization: Bearer access_token
 PARAMETERS:
  catalogue_id: UUID

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | ProductInCatalogueDto[]
```
### Получить список заказанных продуктов
```
REQUEST:
 HTTP VERB: GET
 URL: /products
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | ProductInOrderDto[]
```
### Получить список каталогов продуктов
```
REQUEST:
 HTTP VERB: GET
 URL: /catalogues
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | CatalogueDto[]
```
### Получить список доставок заказов
```
REQUEST:
 HTTP VERB: GET
 URL: /deliveries
 HEADERS:
  Authorization: Bearer access_token

RESPONSE:
 HTTP CODES:
  200: success
  404: not found
  422: unprocessable entity
 BODY FORMAT: { error: HttpException } | DeliveryDto[]
```