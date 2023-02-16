# JPQL 중급 문법

### 1. 경로 표현식

### 1-1. 용어

점(.)을 찍어 객체 그래프를 탐색하는 것

```sql
SELECT m.username -- 상태 필드
FROM Member m
JOIN m.team t -- 단일 값 연관 필드
JOIN m.orders o -- 컬렉션 값 연관 필드
WHERE t.name = '팀A'
```

- 상태 필드 (state field): 단순 값을 저장하기 위한 필드

- 연관 필드 (association field): 연관 관계를 위한 필드

    - 단일 값 연관 필드: 엔티티 대상
    >`@ManyToOne`, `@OneToOne`

    - 컬렉션 값 연관 필드: 컬렉션 대상
    > `@OneToMany`, `@ManyToMany`

&nbsp;

### 1-2. 특징

- 상태 필드는 경로 탐색의 끝. 추가 탐색 불가

- 단일 값 연관 경로에는 **묵시적 내부 조인(INNER JOIN)** 발생. 엔티티 대상이므로 추가 탐색 가능

    > **묵시적 내부 조인**: JPQL 경로 표현식에 의해 SQL에 조인이 발생하는 것 
    > 
    > 쿼리 튜닝에 어려움을 주는 요소로, 되도록이면 묵시적 내부 조인이 발생하지 않게 쿼리를 작성하는 것이 중요함.
    > 
    > (조인은 성능에 영향을 많이 주는데, 쿼리를 봤을 때 직관적으로 조인 여부가 잘 안보이면 문제 소지가 있음)
    > 
    > ```sql
    > SELECT m.team FROM Member m -- JPQL
    >
    > SELECT t.* FROM Member m INNER JOIN Team t ON m.team_id = t.id -- SQL
    > ```

- 컬렉션 값 연관 경로에도 묵시적 내부 조인 발생. 그러나 컬렉션 대상이므로 추가 탐색 불가

    > FROM 절에서 **명시적 조인**을 통해 별칭을 얻으면 별칭을 가지고 추가 탐색 가능
    > 
    > ```sql
    > SELECT t.members FROM Team t -- t.members 는 컬렉션이라 추가 탐색 X
    > 
    > SELECT m.username FROM Team t JOIN t.members m -- 별칭 m을 얻어 m에 대해서는 추가 탐색 O
    > ```

&nbsp;

다시 한번 강조하면, 가급적 묵시적 조인을 쓰지 않는 것을 권장함. 명시적 조인을 사용하자.

&nbsp;

### 2. 페치 조인 (fetch join)

👉 **매우 중요한 개념**

표준 SQL의 조인이 아닌, JPQL에서 성능 최적화를 위해 제공하는 전용 기능.

연관된 엔티티나 컬렉션을 한방 쿼리로 함께 조회할 수 있게 해줌. (N + 1 문제 해결)

문법은 "JOIN FETCH" 로 사용

&nbsp;

### 2-1. 엔티티 페치 조인

연관된 엔티티를 함께 가져오는 것

Member 엔티티를 조회하는데, 연관 관계 매핑(N:1)된 엔티티 Team도 한번에 같이 조회하고자 함

> Member 엔티티 내에서 Team은 지연 로딩 전략으로 설정되어 있다고 가정

```sql
SELECT m FROM Member m JOIN FETCH m.team -- JPQL

SELECT m.*, t.* FROM Member m INNER JOIN Team t ON m.team_id = t.id -- SQL
```

JPQL 상으로는 SELECT 절에 Member 엔티티만 프로젝션 되어있는데, JOIN FETCH로 Team을 지정하니 변환된 SQL에는 SELECT 절에 Member와 Team 엔티티 모두 다 프로젝션 되는 것을 알 수 있음.

&nbsp;

### 2-2. 컬렉션 페치 조인

연관된 컬렉션을 함께 가져오는 것

Team 엔티티를 조회하는데, 연관 관계 매핑(1:N)된 엔티티 Member를 담은 컬렉션도 한번에 같이 조회하고자 함

```sql
SELECT t FROM Team t JOIN FETCH t.members -- JPQL

SELECT t.*, m.* FROM Team t INNER JOIN Member m ON t.id = m.team_id -- SQL
```

&nbsp;

### 페치 조인과 DISTINCT

JPQL에서 DISTINCT를 사용하면 2가지 기능을 함

- SQL에 DISTINCT 적용

- 반환된 결과에서 중복된 엔티티가 있다면 제거해줌

&nbsp;

2-2의 예시 쿼리로 반환된 결과에는 동일한 Team 엔티티가 중복되어 있을 수 있음

> 가령, Member1도 TeamA 소속이고 Member2도 TeamA 소속이면 INNER JOIN 시 생성된 조인 테이블의 row가 2개가 되므로, TeamA가 2번 반환됨
> 
> 이렇게 1:N 연관 관계인 경우, JOIN에서 데이터 개수가 뻥튀기 되는 현상들이 발생함 

이 때, SELECT 절에 DISTINCT를 추가하면 애플리케이션에 반환된 데이터에서 중복된 엔티티는 지워짐

> SQL로 반환된 조인 테이블 상에서는 DISTINCT를 추가했음에도 불구하고 중복이 제거되지 않음.
> 
> 두 row가 모든 column에 대해 완전히 동일하지는 않기 때문

&nbsp;

### 2-3. 페치 조인 vs 일반 조인

일반 조인은 연관된 엔티티를 함께 조회하지 않음 (아래는 일반 JOIN의 예시)

```sql
SELECT t FROM Team t JOIN t.members m -- JPQL

SELECT t.* FROM Team t INNER JOIN Member m ON t.id = m.team_id -- SQL
```

페치 조인을 사용했을 때와는 다르게, 변환된 SQL의 SELECT 절에 연관된 엔티티가 프로젝션되지 않음

&nbsp;

### 2-4. 페치 조인의 한계

- 페치 조인 대상에는 별칭을 줄 수 없음

> Hibernate에서는 가능은 하지만 **권장되지 않음**
> 
> 별칭을 사용해서 WHERE 문으로 페치 조인 대상을 필터링 하는 등의 행위는 객체 그래프 탐색의 기본 사상과 일치하지 않음. 객체 그래프 탐색은 탐색된 대상(t.members라면 members 객체)의 모든 데이터를 가져오는 것을 기본으로 하기 때문에 그 중 일부(members 중 몇 개의 member)만을 취하는 것은 전제하고 있지 않음.
> 
> 예외적으로 사용하는 경우는, 별칭 사용이 데이터 정합성을 깨뜨리지 않을 때.
> 
> 또는 연쇄적인 페치 조인(team &#8594; members &#8594; ...)을 하고자 할 때 사용할 수는 있음.

```sql
SELECT t FROM Team t JOIN FETCH t.members m WHERE m.age > 25 -- (X) t.members에 별칭을 사용하지 말자
```

- 둘 이상의 컬렉션은 페치 조인할 수 없음

> 다시 말해, 페치 조인은 오직 컬렉션 하나만을 대상으로 사용할 수 있음
> 
> 1:N 관계인 경우 조회 시 데이터 개수가 부풀려지는데 (ex. Team에 Member 여럿이 있다면 같은 Team 데이터가 여러번 중복), 컬렉션을 2개 이상 같이 가져오는 순간 이 데이터 뻥튀기가 기하급수적으로 높아지기 때문.

- 컬렉션을 페치 조인하면 페이징 API(`setFirstResult()`, `setMaxResults()`)를 사용할 수 없음

> 1:1, N:1 처럼 단일값 연관 필드들은 페치 조인해도 페이징이 가능함 (데이터 뻥튀기가 발생하지 않으므로)
> 
> 그런데 컬렉션 같은 다수 데이터가 있는 연관 필드는 페치 조인하고 페이징을 요청하면, 모든 데이터를 받아와 메모리에서 페이징 작업을 하게 됨. 컬렉션의 데이터의 숫자가 많으면 많을수록 메모리 사용량은 늘어나고 성능은 저하됨.
> 
> 그래서 Hibernate의 경우, 컬렉션 페치 조인과 페이징을 같이 사용하면 다음과 같이 경고해줌
> 
>     WARN: HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!
>
> DB에 나간 쿼리를 확인해보면 페이징 쿼리는 포함이 안되어있음 (일단 데이터를 전부 받아오고 메모리에서 페이징한다는 증거)

&nbsp;

그렇다고 컬렉션 데이터를 페이징할 때 페치 조인을 하지 않으면 N+1 문제가 있음

```java
String query = "SELECT t FROM Team t";
List<Team> result = em.createQuery(query, Team.class)
        .setFirstResult(0)
        .setMaxResults(2)
        .getResultList();
```

> Team 엔티티가 N개 반환되었을 때, 이후에 Team에 속한 members(LAZY)의 데이터를 필요로 하는 로직이 실행되면 N번 만큼의 추가 쿼리가 발생함.

이 때는 Batch Size 옵션을 사용하면 됨 (2가지 방법)

> size는 상황에 따라 적절하게 설정하되, 너무 크면 메모리 사용량이 높아지거나 터질 수도 있으므로 주의

1. `@BatchSize` 어노테이션 사용

```java
@Entity
class Team {
  ...
  @BatchSize(100)
  @OneToMany(mappedBy = "team")
  private List<Member> members = new ArrayList<>();
}
```

2. JPA 설정 파일(persistence.xml)에 옵션 추가

```xml
<property name="hibernate.default_batch_fetch_size" value="100" />
```

Batch Size 옵션을 적용한 뒤 DB에 나간 쿼리를 살펴보면 다음과 같은 형태임.

```sql
SELECT ... FROM Member m WHERE m.TEAM_ID IN (?, ?, ..., ?)
```

IN절에 오는 집합에 Team의 id들이 들어있는데, 이 때 집합의 크기를 결정하는 것이 Batch Size 옵션임.

members(LAZY)의 데이터가 요구되는 시점에, 쿼리 효율성을 위해 하나의 Team이 아니라 여러 Team에 대해 각 Team에 소속된 Member 엔티티들을 한번에 조회해오는 것.

만약 Batch Size가 100인데 Team은 150개라면, 100개의 team_id 에 소속된 Member들을 조회하는 배치 쿼리 1회, 나머지 50개에 대한 배치 쿼리 1회 해서 총 2회 날림

결과적으로 N+1 문제 때문에 (1 + 150)개가 될 뻔한 쿼리를, Team을 조회하는 쿼리 1회에다가 배치 쿼리 2회로 총 3번의 쿼리로 해결.

&nbsp;

### 2-5. 정리

- 성능 최적화가 필요한 곳은 대부분 페치 조인을 통해 해결 가능 (그만큼 페치 조인이 매우 중요한 개념)

- 객체 그래프를 유지할 때 사용하면 효과적 (m.team, t.members)

- 단, 페치 조인이 만능은 아니기 때문에 일반 조인을 사용하는 것이 효과적일 때도 있음

  > 여러 테이블을 조인해서 엔티티가 가진 모양과는 전혀 다른 결과를 내야할 때는 페치 조인보다 일반 조인을 사용하고, 필요한 부분만 조회해서 DTO로 반환하는 것이 효과적.

&nbsp;

### 3. 다형성 쿼리

### 3-1. TYPE

조회 대상을 특정 하위 타입으로 한정할 때 사용

ex) Item의 하위 타입 Album, Book, Movie 중에서 Book과 Movie만 조회

```sql
SELECT i FROM Item i WHERE TYPE(i) IN (Book, Movie) -- JPQL

SELECT i FROM Item i WHERE i.DTYPE IN ('Book', 'Movie') -- SQL
```

&nbsp;

### 3-2. TREAT

상위 타입을 특정 하위 타입으로 다룰 때 사용 (자바의 다운 캐스팅과 유사)

SELECT(Hibernate), FROM, WHERE 절에서 사용 가능

```sql
SELECT i FROM Item i WHERE TREAT(i AS Book).author = 'Andy' -- JPQL

SELECT i.* FROM Item i WHERE i.DTYPE = 'Book' AND i.author = 'Andy' -- SQL
```

> 예시의 SQL은 싱글 테이블 전략의 경우를 나타냄. (조인 전략이나 별도 테이블 전략이면 쿼리가 달라짐)

&nbsp;

### 4. 엔티티 직접 사용

### 4-1. PK 값으로 치환

JPQL에서 엔티티를 직접 사용하면 SQL에서는 해당 엔티티의 PK 값을 사용함.

```sql
SELECT COUNT(m) FROM Member m -- JPQL

SELECT COUNT(m.id) FROM Member m -- SQL
```

```sql
SELECT m FROM Member m WHERE m = :member -- JPQL

SELECT m.* FROM Member m WHERE m.id = ? -- SQL
```

### 4-2. FK 값으로 치환

연관된 엔티티를 직접 사용하면 연관된 엔티티를 식별할 FK 값을 사용함.

```sql
SELECT m FROM Member m WHERE m.team = :team -- JPQL

SELECT m.* FROM Member m WHERE m.TEAM_ID = ? -- SQL
```

&nbsp;

### 5. Named 쿼리

JPQL 쿼리를 미리 작성해놓고 이름을 지어주면, 이름으로 호출해서 쿼리를 사용할 수 있음

- 정적 쿼리만 사용 가능

- 애플리케이션 로딩 시점에 초기화

> 로딩 시점에 SQL로 파싱한 다음 캐싱해서 이후에 사용될 때의 비용을 줄여줌 (파싱하는 비용)

- 애플리케이션 로딩 시점에 쿼리 검증

> 유효하지 않은 쿼리에 대해 `QuerySyntaxException`을 발생시켜줌

&nbsp;

### 5-1. 네임드 쿼리 선언 방식

1. `@NamedQuery` 어노테이션 사용

```java
@Entity
@NamedQuery(
        name = "Member.findByUsername"
        query = "SELECT m FROM Member m WHERE m.username = :username"
)
class Member {
  ...
}
```

정의해둔 네임드 쿼리는 `createNamedQuery()` 메서드를 통해 사용

```java
List<Member> result = em.createNamedQuery("Member.findByUsername", Member.class)
        .setParameter("username", "Andy")
        .getResultList();
```

&nbsp;

2. XML에 정의

```xml
<!--persistence.xml-->
<persistence-unit name="basic">
    <mapping-file>META-INF/ormMember.xml</mapping-file>
</persistence-unit>
```

```xml
<!--ormMember.xml-->
<entity-mappings xmlns="http://xmlns.jcp.org/xml/ns/persistence/orm" version="2.1">
  <named-query name="Member.findByUsername">
  <query><![CDATA[
      select m
      from Member m
      where m.username = :username
      ]]></query>
  </named-query>

  <named-query name="Member.count">
  <query>select count(m) from Member m</query>
  </named-query>
</entity-mappings>
```

META-INF 디렉토리 하위에 네임드 쿼리를 정의할 XML을 생성하고, persistence.xml에 해당 파일을 매핑해주면 됨.

XML 정의가 어노테이션 정의보다 우선권을 가짐

> 애플리케이션 운영 환경에 따라 다른 네임드 쿼리 XML을 배포하는 방법을 취할 수 있음

&nbsp;

### 5-2. Spring Data JPA의 네임드 쿼리

어노테이션 방식은 엔티티 클래스 코드가 너무 지저분해지고, XML 방식은 번거로움.

그래서 Spring Data JPA를 사용하면 DAO나 인터페이스에 직접 네임드 쿼리를 정의할 수 있는데, 이 방법이 가장 깔끔

```java
public interface UserRepository extends JpaRepository<User, Long> {

  @Query("select u from User u where u.emailAddress = ?1")
  User findByEmailAddress(String emailAddress);
}
```

> Spring Data JPA는 JPA를 추상화시킨 기술이기 때문에 `@Query`가 Named Query 생성을 담당한다고 보면 됨.

&nbsp;

### 6. 벌크 연산

단건이 아닌 여러건의 UPDATE, DELETE 쿼리를 한방에 해결

가령 재고가 10개 미만인 모든 상품의 가격을 10% 인상하려고 할 때, JPA의 변경 감지 기능으로 UPDATE를 하기엔 너무 비효율적임.

1. 상품 테이블에서 재고 10개 미만인 모든 상품 조회

2. 반복문으로 모두 다 가격 10% 인상

3. 트랜잭션 커밋 시점에 JPA가 변경을 감지해서 UPDATE 쿼리를 날림

변경할 상품이 100건이라면 UPDATE 쿼리도 100번 나가야함

이 때 필요한 것이 쿼리 한방으로 다수의 row를 변경시킬 수 있는 벌크 연산 기능.

```java
String query = "UPDATE Product p SET p.price = p.price * 1.1 WHERE p.stock < 10";

int resultCount = em.createQuery(query)
        .executeUpdate();
```

JPA 표준에는 없지만 Hibernate의 경우 UPDATE, DELETE 뿐만 아니라 "INSERT INTO ... SELECT ..."도 지원함

&nbsp;

### 6-1. 주의 사항

벌크 연산은 영속성 컨텍스트를 무시하고 DB에 직접 쿼리함

따라서 영속성 컨텍스트와 DB 간의 데이터 정합성을 염두에 두어야함

&nbsp;

해결 방안

- 벌크 연산을 먼저 실행

  > 영속성 컨텍스트에 관련된 데이터가 없을 때, 벌크 연산 먼저하고 컨텍스트를 사용하는 다른 작업을 하면 됨

- 벌크 연산 실행 후 영속성 컨텍스트 초기화

  > 영속성 컨텍스트에 관련된 데이터가 있다면, 벌크 연산 이후 DB와 데이터 싱크가 안맞을 수 있으므로 `clear()` 후 데이터를 다시 조회해와야 함

&nbsp;

아래 예시에서 만약 `clear()`를 안했다면 업데이트 이전의 값이 출력됨

```java
Member member = new Member();
member.setUsername("Andy");
em.persist(member);

// 이 시점에 내부적으로 영속성 컨텍스트 flush
int resultCount = em.createQuery("UPDATE Member m SET m.username = 'Lee'")
        .executeUpdate();

em.clear(); // 영속성 컨텍스트 초기화
        
// 영속성 컨텍스트가 초기화되어 DB에서 새로 데이터를 조회해옴
Member findMember = em.find(Member.class, member.getId());
System.out.println(findMember.getUsername()); // Lee

tx.commit();
```

> (참고) 내부적으로 자동 flush가 되는 타이밍
> 
>- 트랜잭션 커밋 시
>- 쿼리 실행 직전 (`executeQuery()`, `executeUpdate()`)
>- `flush()` 직접 호출 시

&nbsp;

### 6-2. Spring Data JPA의 벌크 연산

`@Modifying` 어노테이션은 마킹된 메서드가 데이터 변경(UPDATE, DELETE) 작업을 한다는 것을 명시함.

```java
@Modifying(clearAutomatically = true)
@Query("update User u set u.firstname = ?1 where u.lastname = ?2")
int setFixedFirstnameFor(String firstname, String lastname);
```

이 때 clearAutomatically 옵션(기본값 false)을 true로 설정하면, 쿼리가 실행된 후에 영속성 컨텍스트를 자동으로 clear 해줌.