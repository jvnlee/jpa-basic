# JPQL 기본 문법

### 1. JPA가 지원하는 쿼리 방법

### 1-1. JPQL

```java
String jpql = "select m from Member m where m.age > 20";
List<Member> result = em.createQuery(jpql, Member.class).getResultList();
```

- SQL을 추상화한 객체 지향 쿼리 언어

  > JPA를 활용해 자바 애플리케이션을 개발할 때, JPA가 지원하는 RDB의 종류에 상관 없이 데이터를 다룰 수 있게 도와줌

- 엔티티 객체를 대상으로 쿼리

  > 엔티티 객체를 대상으로 쿼리를 작성하면 엔티티의 매핑 정보를 토대로 SQL로 변환해서 DB로 쿼리함

- 기본적으로 ANSI 표준 SQL이 지원하는 문법은 모두 지원

&nbsp;

### 1-2. Criteria

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Member> query = cb.createQuery(Member.class);

Root<Member> m = query.from(Member.class); // 조회를 시작할 클래스 지정
CriteriaQuery<Member> cq = query.select(m); // 기본 조회 쿼리 생성

if (...) {
    cq = cq.where(cb.greaterThan(m.get("age"), 20)); // 조건에 따라 동적으로 쿼리 추가
}

List<Member> result = em.createQuery(cq).getResultList();
```

- 쿼리를 메서드 체이닝 형태로 작성할 수 있음

  > 쿼리 작성하다가 실수하더라도 자바 코드이기 때문에 컴파일 에러로 잡아낼 수 있음

- 동적 쿼리 생성이 일반 JPQL보다는 용이함

  > 쿼리 문자열을 띄어쓰기 신경써가며 자르고 붙이고 할 필요 없음

- 그러나 생각보다 복잡하고 알아보기 어려워서 실제로 많이 사용하지는 않음

  > Criteria 대신 QueryDSL을 사용하면 됨

&nbsp;

### 1-3. QueryDSL

```java
JPAQueryFactory queryFactory = new JPAQueryFactory(em);
QMember m = QMember.member; // QueryDSL이 생성한 Q타입 내부의 static 객체

List<Member> result = queryFactory.selectFrom(m)
        .where(m.age.gt(20))
        .orderBy(m.name.desc())
        .fetch();
```

- 자바 코드와 메서드 체이닝 방식으로 쿼리를 작성 (Criteria 보다 이게 훨씬 단순하고 보기 편함)

- 컴파일 시점에 쿼리 관련 실수를 캐치할 수 있음

- 동적 쿼리 작성이 매우 편리함

- 실무 사용 권장

&nbsp;

### 1-4. 네이티브 SQL

```java
String sql = "SELECT ID, AGE, NAME FROM MEMBER WHERE NAME = 'Andy'";
List<Member> result = em.createNativeQuery(sql, Member.class)
        .getResultList();
```

- SQL을 직접 사용하는 것

- JPQL로 해결할 수 없는 특정 DB에 의존적인 기능

  > ex) Oracle의 CONNECT BY, SQL hint

&nbsp;

### 1-5. JDBC와 Spring JdbcTemplate

- JPA를 사용하면서 JDBC 커넥션을 직접 사용하거나 SpringJdbcTemplate, MyBatis 등 혼용 가능

- 단, 영속성 컨텍스트를 적절한 시점에 flush 해주는 것이 필요함 (`em.flush()`)

  > ex) JPA 기술로 데이터를 영속화하고, JDBC 기술로 해당 데이터를 조회하면 조회 불가능
  > 
  > 보통은 커밋 시점에 flush가 되므로 DB에 곧바로 반영이 안되기 때문

&nbsp;

### 2. JPQL 기본 문법과 기능

### 2-1. 기본 문법

- 기본 문법은 ANSI SQL 표준 문법과 동일함

- 엔티티와 속성 대소문자 구분함 (ex. Member, age)

- 키워드는 대소문자 구분 안함 (ex. SELECT, where)

- 엔티티를 지칭할 별칭(as는 생략 가능)을 필수로 사용해야함 (ex. Member m)

&nbsp;

### 2-2. TypedQuery와 Query

- `TypedQuery`: 반환 타입이 명확할 때

  ```java
  TypedQuery<Member> query = em.createQuery("SELECT m FROM Member m", Member.class);
  ```

  > Member 엔티티 전체를 조회하는 것이기 때문에 Member 타입으로 명시할 수 있음

&nbsp;

- `Query`: 반환 타입이 명확하지 않을 때

  ```java
  Query query = em.createQuery("SELECT m.username, m.age FROM Member m");
  ```

  > username은 String, age는 int 타입이라 한가지 타입을 명시할 수 없음

&nbsp;

### 2-3. 결과 조회

- `getResultList()`: 결과가 하나 이상일 때, `List` 반환

  ```java
  List<Member> result = query.getResultList();
  ```

  > 결과가 없으면 빈 List 반환

- `getSingleResult()`: 결과가 오직 하나일 때, 단일 객체 반환

  ```java
  Member result = query.getSingleResult();
  ```

  > 결과가 없으면 `NoResultException`
  > 
  > 결과가 둘 이상이면 `NonUniqueResultException`

&nbsp;

### 2-4. 파라미터 바인딩

- 이름 기준

  ```java
  Member result = em.createQuery("SELECT m FROM Member m WHERE m.username = :username", Member.class)
        .setParameter("username", "Andy");
        .getSingleResult();
  ```
  
- 위치 기준 (권장하지 않음)

  ```java
  Member result = em.createQuery("SELECT m FROM Member m WHERE m.username = ?1", Member.class)
        .setParameter(1, "Andy");
        .getSingleResult();
  ```

&nbsp;

### 2-5. 프로젝션

SELECT 절에 조회 대상을 지정하는 것

> 일반적인 SQL은 SELECT 절에 스칼라 타입만 들어갈 수 있지만, JPQL은 다양한 대상을 넣을 수 있음

프로젝션 대상:

- 엔티티

  > SELECT m FROM Member m
  > 
  > SELECT m.team FROM Member m

- 임베디드 타입

  > SELECT m.address FROM Member m

- 스칼라 타입 (숫자, 문자열 등 기본 타입)

  > SELECT m.username, m.age FROM Member m

&nbsp;

### 프로젝션 - 여러 값 조회

```sql
SELECT m.username, m.age FROM Member m
```

여러 값을 조회하는 경우, 반환 타입을 한가지로 특정할 수 없음

이 때는 3가지 방법이 있음

1. `Query` 타입으로 조회

```java
List result = em.createQuery("SELECT m.username, m.age FROM Member m")
        .getResultList();

Object o = result.get(0);
Object[] data = (Object[]) o;

data[0] // username
data[1] // age
```

2. `Object[]` 타입으로 조회

```java
List<Object[]> result = em.createQuery("SELECT m.username, m.age FROM Member m")
        .getResultList();

Object[] data = result.get(0);

data[0] // username
data[1] // age
```

3. new 명령어로 조회

username과 age를 필드로 갖는 MemberDto가 있다고 가정

```java
List<MemberDto> result = em.createQuery("SELECT new jpabasic.jpa.dto.MemberDto(m.username, m.age) FROM Member m", MemberDto.class)
        .getResultList();

MemberDto memberDto = result.get(0);

memberDto.getUsername() // username
memberDto.getAge() // age
```

주의:

- new 명령어 뒤에 오는 클래스 이름은 패키지명까지 포함한 풀네임을 적어야함

- 해당 DTO 클래스에 각 데이터의 순서와 타입이 일치하도록 생성자가 필요함

&nbsp;

### 2-6. 페이징

- `setFirstResult(int startPosition)`: 조회 시작 위치

- `setMaxResults(int maxResult)`: 조회할 데이터의 수

```java
List<Member> result = em.createQuery("SELECT m FROM Member m ORDER BY m.age DESC", Member.class)
        .setFirstResult(0) // 0번째 데이터부터
        .setMaxResults(10) // 10개를 가져옴
        .getResultList();
```

> JPQL이 SQL로 번환될 때, (MySQL 기준)
> 
> ```java
> setFirstResult(0): "... LIMIT ?" // OFFSET이 0이므로 생략
> 
> setFirstResult(0이 아닌 수): "... LIMIT ? OFFSET ?"
> ```

&nbsp;

### 2-7. 조인

- 내부 조인

  ```sql
  SELECT m FROM Member m (INNER) JOIN m.team t 
  ```

  > INNER JOIN에서 INNER 생략 가능


- 외부 조인

  ```sql
  SELECT m FROM Member m LEFT (OUTER) JOIN m.team t 
  ```

  > LEFT OUTER JOIN에서 OUTER 생략 가능


- 세타 조인

  ```sql
  SELECT COUNT(m) FROM Member m, Product p WHERE m.username = p.name 
  ```

  > 서로 연관 관계가 없는 엔티티끼리 조인하는 경우 (JOIN문 없이 작성)
  >
  > 대부분의 조인은 연관 관계가 있는 것끼리 이루어지므로 (내부, 외부 조인) 세타 조인은 거의 쓰이지 않음

&nbsp;

### ON 절을 활용한 조인 (JPA 2.1 이상)

- 조인 대상 필터링

  ex) Member와 Team을 조인하되, 팀명이 A인 Team만 조인하고 싶음

  &nbsp;

  JPQL:
  ```sql
  SELECT m, t FROM Member m LEFT JOIN m.team t ON t.name = 'A'
  ```

  SQL:
  ```sql
  SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID = t.id AND t.name = 'A'
  ```

&nbsp;

- 연관 관계 없는 엔티티 간 외부 조인 (Hibernate 5.1 이상)

  ex) Member와 Product 중 이름이 같은 것 외부 조인
  
  &nbsp;
  
  JPQL:
    ```sql
    SELECT m, p FROM Member m LEFT JOIN Product p ON m.username = p.name
    ```
  
  SQL:
    ```sql
    SELECT m.*, t.* FROM Member m LEFT JOIN Product p ON m.TEAM_ID = t.id AND t.name = 'A'
    ```

&nbsp;

### 2-8. 서브 쿼리

SQL에서와 마찬가지로 메인 쿼리에서 사용한 엔티티는 서브쿼리에서 재사용하기 보다는 새로 정의하는게 성능 상 좋음

ex) age가 평균보다 높은 Member 조회

```sql
SELECT m FROM Member m WHERE m.age > (SELECT AVG(m2.age) FROM Member m2)
```

&nbsp;

ex) 한 건 이상 주문한 고객 조회

```sql
SELECT m FROM Member m WHERE (SELECT COUNT(o) FROM Order o WHERE m = o.member) > 0
```

&nbsp;

### 서브 쿼리 지원 함수

- (NOT) EXISTS + 서브 쿼리: 서브 쿼리에 결과가 존재하면 true

  ex) A라는 이름의 Team에 소속된 Member
  
  ```sql
  SELECT m FROM Member m WHERE EXISTS (SELECT t FROM m.team t WHERE t.name = 'A')
  ```

- ALL + 서브쿼리: 서브 쿼리 모두 만족하면 true

  ex) 모든 Product 각각의 재고보다 주문량이 높은 Order

  ```sql
  SELECT o FROM Order o WHERE o.orderAmount > ALL (SELECT p.stock FROM Product p)
  ```

- ANY, SOME + 서브쿼리: 서브 쿼리에서 하나라도 만족하면 true

  ex) 어느 팀이든 팀에 소속된 Member

  ```sql
  SELECT m FROM Member m WHERE m.team = ANY (SELECT t FROM Team t)
  ```

- (NOT) IN + 서브 쿼리: 서브 쿼리 결과 중 하나라도 같은 것이 있으면 true

  ex) 이름이 10글자 미만인 Team에 소속된 Member 

  ```sql
  SELECT m FROM Member m WHERE m.team IN (SELECT t FROM Team t WHERE LENGTH(t.name) < 10)
  ```
  
&nbsp;

### 서브 쿼리 제약

- JPA 표준: WHERE 절과 HAVING 절에서만 서브 쿼리 사용 가능

- Hibernate: SELECT 절에서도 서브 쿼리 사용 가능

&nbsp;

주의: JPQL은 FROM 절에는 서브 쿼리 사용 불가

해결 방안:

- JOIN 사용

- 네이티브 쿼리 사용

- 메인 쿼리와 서브 쿼리를 따로 날려서 얻어온 결과를 애플리케이션 코드에서 조작

> 서브 쿼리 대신 JOIN을 사용할 수 있다면 JOIN을 사용하자
>
> 두 방식이 비슷한 결과를 가져올 수는 있어도, 서브 쿼리는 JOIN에 비해 성능이 안 좋음. 사용자가 서브 쿼리를 사용하면 MySQL도 내부적으로 JOIN문으로 변환해서 실행함. 내부적으로 변환을 시켜주긴 하지만 애초에 서브 쿼리 남용에 주의할 필요가 있음.

&nbsp;

### 2-9. JPQL 타입 표현

- 문자: 'HELLO', 'Andy''s'

  > 문자 내에 quotation이 들어가는 경우 두개 넣어주면 됨

- 숫자: 10L(Long), 10D(Double), 10F(Float)

- Boolean: TRUE, FALSE

- Enum: JPQL 쿼리 문자열에 표기할 때는 패키지명까지 포함한 풀네임

  ```sql
  SELECT m.username FROM Member m WHERE m.type = jpabasic.jpa.enum.MemberType.ADMIN
  ```
  
  > setParameter()로 파라미터 삽입하는 경우에는 풀네임을 안써도 됨
  >
  > ```java
  > String jpql = "SELECT m.username FROM Member m WHERE m.type = :memberType";
  >
  > List<Member> result = em.createQuery(jpql)
  >        .setParameter("memberType", MemberType.ADMIN)
  >        .getResultList();
  >```

- Entity 타입: 상속 관계에 있는 엔티티에서 DTYPE 체크할 때 아래와 같이 사용
  
  ```sql
  SELECT i FROM Item i WHERE TYPE(i) = Book
  ```
  
&nbsp;

### 2-10. 조건식

- 기본 CASE 문

  ```sql
  SELECT
      CASE WHEN m.age <= 20 THEN '학생 요금'
           WHEN m.age >= 60 THEN '경로 요금'
           ELSE '일반 요금'
      END
  FROM Member m
  ```

- 단순 CASE 문

  ```sql
  SELECT
      CASE t.name
           WHEN '팀A' THEN '인센티브 110%'
           WHEN '팀B' THEN '인센티브 120%'
           ELSE '인센티브 105%'
      END
  FROM Team t
  ```
  
- COALESCE

  하나씩 조회해서 값을 반환하되, 값이 null이면 두번째 인자 값 반환
  
  ```sql
  SELECT COALESCE(m.username, '이름 없는 회원') FROM Member m
  ```

- NULLIF

  하나씩 조회해서 값을 반환하되, 값이 두번째 인자 값과 같으면 null을 반환
  
  ```sql
  SELECT NULLIF(m.username, '관리자') FROM Member m
  ```
  
&nbsp;

### 3. JPQL 기본 함수

- CONCAT, SUBSTRING, TRIM

- LOWER, UPPER

- LENGTH

- LOCATE

- ABS, SQRT, MOD

다 기본 SQL과 사용 방식이 같고, 다음 2가지는 JPA 전용 함수

- SIZE: 인자의 컬렉션 길이를 반환

  ```sql
  SELECT SIZE(t.members) FROM Team t
  ```

- INDEX: `@OrderColumn`으로 순서값(인덱스)이 별도 저장되어있는 컬렉션 타입을 조회할 때 원소의 인덱스를 나타냄

  > 애초에 `@OrderColumn` 자체를 권장하지 않기 때문에 INDEX 함수도 쓸 일이 거의 없음
  
  ```sql
  SELECT m FROM Member m WHERE INDEX(m.phoneNumber) = 1
  ```

&nbsp;

- 사용자 정의 함수

등록 방법:

1. 커스텀 Dialect 추가

```java
class MyH2Dialect extends H2Dialect {
    
    public MyH2Dialect() {
        registerFunction("newFunc", new StandardSQLFunction("newFunc", StandardBasicTypes.STRING));
    }
}
```

2. JPA 설정 파일에서 Dialect를 커스텀 Dialect로 변경 (persistence.xml)

3. JPQL 쿼리에서 사용

JPA 표준
```sql
SELECT FUNCTION('newFunc', m.username) FROM Member m
```

Hibernate
```sql
SELECT newFunc(m.username) FROM Member m
```