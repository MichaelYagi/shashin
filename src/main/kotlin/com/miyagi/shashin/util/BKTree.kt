package com.miyagi.shashin.util

import dev.brachtendorf.jimagehash.hash.Hash

// Usage
//val tree = BKTree { h1, h2 -> h1.hammingDistance(h2) }
//
//// Insert entries
//val entry1 = BKTree.HashEntry(1, Hash(BigInteger("1234567890"), 64, 1))
//val entry2 = BKTree.HashEntry(2, Hash(BigInteger("9876543210"), 64, 1))
//tree.insert(entry1)
//tree.insert(entry2)
//
//// Search
//val query = Hash(BigInteger("1234567891"), 64, 1)
//val duplicates = tree.search(query, threshold = 5)
//for (dupe in duplicates) {
//    println("Duplicate found: id=${dupe.id}, distance=${query.hammingDistance(dupe.hash)}")
//}
//
//// Update hash
//val newHash = Hash(BigInteger("1111111111"), 64, 1)
//val success = tree.updateHash(1, newHash)
//println("Update success: $success")


class BKTree(private val distanceFunc: (Hash, Hash) -> Int) {

    // Member data class lives inside BKTree
    data class HashEntry(
        val id: String,      // image ID from SQLite
        var hash: Hash    // perceptual hash object
    )

    private class Node(val entry: HashEntry) {
        val children: MutableMap<Int, Node> = mutableMapOf()
    }

    private var root: Node? = null

    fun insert(entry: HashEntry) {
        if (root == null) {
            root = Node(entry)
            return
        }
        var current = root!!
        var dist = distanceFunc(entry.hash, current.entry.hash)
        while (current.children.containsKey(dist)) {
            current = current.children[dist]!!
            dist = distanceFunc(entry.hash, current.entry.hash)
        }
        current.children[dist] = Node(entry)
    }

    fun search(query: Hash, threshold: Int): List<HashEntry> {
        val results = mutableListOf<HashEntry>()
        fun recurse(node: Node) {
            val dist = distanceFunc(query, node.entry.hash)
            if (dist <= threshold) {
                results.add(node.entry)
            }
            for ((edgeDist, child) in node.children) {
                if (edgeDist in (dist - threshold)..(dist + threshold)) {
                    recurse(child)
                }
            }
        }
        root?.let { recurse(it) }
        return results
    }

    // Setter-like update method: change hash for a given ID
    fun updateHash(id: String, newHash: Hash): Boolean {
        var updated = false
        fun recurse(node: Node) {
            if (node.entry.id == id) {
                node.entry.hash = newHash
                updated = true
                return
            }
            for (child in node.children.values) {
                recurse(child)
                if (updated) return
            }
        }
        root?.let { recurse(it) }
        return updated
    }
}

