package com.miyagi.shashin.repository

import com.miyagi.shashin.model.BrowserNameCount
import com.miyagi.shashin.model.OsNameCount
import com.miyagi.shashin.model.Useragent
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UseragentRepository : CrudRepository<Useragent?, Int?> {
    @Query("SELECT COUNT(DISTINCT agent_name) FROM useragent", nativeQuery = true)
    fun countDistinctAgentName(): Int

    @Query("SELECT COUNT(DISTINCT os_name) FROM useragent", nativeQuery = true)
    fun countDistinctOsName(): Int

    @Query("SELECT agent_name as agentName, COUNT(*) AS count FROM useragent GROUP BY agent_name ORDER BY count DESC", nativeQuery = true)
    fun countByAgentName(): MutableIterable<BrowserNameCount>

    @Query("SELECT os_name as osName, COUNT(*) AS count FROM useragent GROUP BY os_name ORDER BY count DESC", nativeQuery = true)
    fun countByOsName(): MutableIterable<OsNameCount>
}