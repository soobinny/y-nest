package com.example.capstonedesign.domain.users.repository;

import com.example.capstonedesign.domain.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UsersRepository
 * -------------------------------------------------
 * - Users 엔티티에 대한 데이터베이스 접근을 담당하는 레포지토리
 * - JpaRepository를 상속받아 기본적인 CRUD 연산이 제공됨
 * - 사용자 이메일로 조회 및 존재 여부 확인 기능 제공
 */
public interface UsersRepository extends JpaRepository<Users, Integer> {

    /**
     * 이메일로 사용자 조회 (삭제되지 않은 사용자만)
     * - 삭제되지 않은 사용자만 반환 (논리 삭제된 사용자는 제외)
     *
     * @param email 사용자 이메일
     * @return 삭제되지 않은 사용자 정보 (Optional)
     */
    Optional<Users> findByEmailAndDeletedFalse(String email);

    /**
     * 이메일로 사용자 존재 여부 확인
     * - 해당 이메일이 이미 존재하는지 여부를 반환
     *
     * @param email 사용자 이메일
     * @return 이메일이 이미 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByEmail(String email);

//    /**
//     * 이름과 지역으로 활성 사용자 조회
//     * - 논리 삭제되지 않은 사용자만 반환
//     * - 대소문자를 구분하지 않도록 LOWER() 함수로 비교
//     *
//     * @param name   사용자 이름
//     * @param region 사용자 지역
//     * @return 이름과 지역이 일치하는 활성 사용자 목록
//     */
//    @Query("""
//   SELECT u FROM Users u
//   WHERE u.deleted = false
//     AND LOWER(u.name) = LOWER(:name)
//     AND LOWER(u.region) = LOWER(:region)
//""")
//    List<Users> findActiveByNameAndRegion(@Param("name") String name, @Param("region") String region);
}
